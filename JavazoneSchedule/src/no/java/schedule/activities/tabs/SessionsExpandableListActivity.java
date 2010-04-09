/*
 * Copyright (C) 2009 Virgil Dobjanschi, Jeff Sharkey, Filip Maelbrancke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.java.schedule.activities.tabs;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import no.java.schedule.R;
import no.java.schedule.activities.adapters.ExpandableSessionsAdapter;
import no.java.schedule.activities.adapters.SessionsAdapter;
import no.java.schedule.activities.adapters.bean.Session;
import no.java.schedule.activities.adapters.beans.Block;
import no.java.schedule.activities.adapters.interfaces.ExpandableAdapterListener;
import no.java.schedule.activities.fullscreen.SessionDetailsActivity;
import no.java.schedule.provider.SessionsContract;
import no.java.schedule.provider.SessionsContract.BlocksColumns;
import no.java.schedule.provider.SessionsContract.Sessions;
import no.java.schedule.provider.SessionsContract.Tracks;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An activity which displays an expandable list
 */
public class SessionsExpandableListActivity extends ExpandableListActivity {

    public static final String EXTRA_CHILD_MODE = "childmode";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_SELECTION_ARGS = "selection_args";

    public static final String STATE_EXPANDED = "expanded";


    public static final int CHILD_MODE_ALL = 0x0;
    public static final int CHILD_MODE_STARRED = 0x1;
    public static final int CHILD_MODE_VISIBLE_TRACKS = 0x2;
    public static final int CHILD_MODE_PICK = 0x3;

    private ExpandableSessionsAdapter adapter;
    private String preferenceKey;

    /** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expandable_list);

        final Intent intent = getIntent();
        int childMode = intent.getIntExtra(EXTRA_CHILD_MODE, CHILD_MODE_ALL);

        Uri uri = Sessions.CONTENT_URI;
        String selection = null;
        String[] selectionArgs = null;
        int mode = SessionsAdapter.MODE_SCHEDULE;
        switch (childMode) {
            case CHILD_MODE_STARRED: {
                selection = Sessions.STARRED + "=?";
                selectionArgs = new String[] {"1"};
                mode = SessionsAdapter.MODE_STARRED;
                break;
            }

            case CHILD_MODE_VISIBLE_TRACKS: {
                selection = Tracks.VISIBLE + "=?";
                selectionArgs = new String[] {"1"};
                break;
            }

            case CHILD_MODE_PICK: {
                selection = intent.getStringExtra(EXTRA_SELECTION);
                selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
                break;
            }

            default: {
                break;
            }
        }
        preferenceKey = ""+mode;

        adapter = new ExpandableSessionsAdapter(
                this,
                uri,
                selection,
                selectionArgs,
                BlocksColumns.TIME_START+","+ SessionsContract.SessionsColumns.TYPE+" ASC",
                mode,
                new ExpandableAdapterListener(){
                    /** {@inheritDoc} */
                    public void onNewData() {
                        ExpandableListView elv = getExpandableListView();
                        int size = adapter.getGroupCount();
                        for( int i=0; i<size; i++) {
                            elv.expandGroup(i);
                        }
                    }
                });

        ExpandableListView listView = getExpandableListView();
        listView.setAdapter(adapter);
        listView.setChildDivider( getResources().getDrawable( android.R.drawable.divider_horizontal_bright));

        // Restore the expanded groups  TODO - fix not working (see @saveCurrentExpandedGroups)
        //restoreExpandedStateFromPreferences(listView);
        setCurrentTimeSelected(listView);

    }


    private void setCurrentTimeSelected(ExpandableListView listView) {
        // iterate through the groups, and set the group with the current time as selected (if possible)
        int objectToSelect = 0;
        int childCount = 0;
        long currentMillis = System.currentTimeMillis();
        // add one hour for GMT+1
        currentMillis += 1000 * 60 * 60;
        long currentUnixTime = currentMillis/1000L;
        long lastSessionEndTime = 0L;
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            Object obj = adapter.getGroup(i);
            if ( obj instanceof Block) {
                listView.expandGroup(i);
                Block bl = (Block) obj;
                if ( (currentUnixTime > bl.getStartTime()) && (currentUnixTime < bl.getEndTime()) ) {
                    objectToSelect = childCount;
                }
                // also check if we're in a timeslot between sessions -> jump to this block
                if ( (lastSessionEndTime != 0L) && (currentUnixTime > lastSessionEndTime) && (currentUnixTime < bl.getStartTime()) ) {
                    objectToSelect = childCount;
                }
                lastSessionEndTime = bl.getEndTime();
            }

            childCount += adapter.getChildrenCount(i)+1;
        }
        listView.setSelection(objectToSelect);
    }

    private List<Integer> getIntListFromPreferenceEntry(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        List<Integer> result = new ArrayList<Integer>();

        String s = prefs.getString(key,null);

        if( s != null) {
            StringTokenizer tok = new StringTokenizer(s, ",");
            while( tok.hasMoreTokens()) {
                result.add(Integer.parseInt(tok.nextToken()));
            }
        }

        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //saveCurrentExpandedGroups();
    }

    private void saveCurrentExpandedGroups() {
        // TODO fix or remove - slow and does not recover. Was inactive before as it was called in onDestroy
        if (adapter != null) {
            // Save the expanded state
            String expanded = listOfExpendadSectionIds();
            saveExpandedGroups(expanded);

            getExpandableListView().setAdapter((ExpandableListAdapter)null);
            adapter.close();
        }
    }

    private void restoreExpandedStateFromPreferences(ExpandableListView listView) {
        List<Integer> expandedGroupIDs = getIntListFromPreferenceEntry(this, STATE_EXPANDED + preferenceKey);

        for (Integer groupID : expandedGroupIDs) {
            if( groupID < adapter.getGroupCount()) {
                listView.expandGroup( groupID);
            }
        }
    }


    private void saveExpandedGroups(String expanded) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefs.putString( STATE_EXPANDED + preferenceKey, expanded);
        prefs.commit();
    }

    private String listOfExpendadSectionIds() {
        String expanded = "";
        ExpandableListView elv = getExpandableListView();
        int length = adapter.getGroupCount();
        for( int i=0; i < length; i++) {
            if( elv.isGroupExpanded( i)) {
                if( expanded.length() > 0) {
                    expanded += ",";
                }
                expanded += i;
            }
        }
        return expanded;
    }

    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        Object selectedChild = adapter.getChild(groupPosition, childPosition);

        if( selectedChild instanceof Session) {
            showSessionDetail((Session)selectedChild);
        } else if ( selectedChild instanceof Block) {
            expandBlock((Block)selectedChild);
        }
        return true;
    }

    private void expandBlock(Block block) {
        Intent intent = new Intent().setClass( this, SessionsListActivity.class);
        intent.setAction( Intent.ACTION_PICK);
        intent.putExtra(SessionsListActivity.EXTRA_CHILD_MODE, SessionsListActivity.CHILD_MODE_PICK);
        intent.putExtra( EXTRA_SELECTION, "(" + BlocksColumns.TIME_START + "=?) AND (" + BlocksColumns.TIME_END + "=?)");
        intent.putExtra( EXTRA_SELECTION_ARGS, new String[] { "" + block.getStartTime(), "" + block.getEndTime() });
        startActivityForResult( intent, 1);
    }

    private void showSessionDetail(Session selectedChild) {
        // Start details activity for selected item
        Intent intent = new Intent( this, SessionDetailsActivity.class);
        intent.setAction( Intent.ACTION_VIEW);
        intent.setData( selectedChild.getUri() );
        startActivityForResult( intent, 0);
    }

    public void collapseAll() {
        ExpandableListView elv = getExpandableListView();
        int size = adapter.getGroupCount();
        for( int i=0; i<size; i++) {
            elv.collapseGroup(i);
        }
    }

    public void expandAll() {
        ExpandableListView elv = getExpandableListView();
        int size = adapter.getGroupCount();
        for( int i=0; i<size; i++) {
            elv.expandGroup(i);
        }
    }

}
