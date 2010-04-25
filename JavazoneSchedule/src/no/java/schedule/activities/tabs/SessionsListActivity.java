/*
 * Copyright (C) 2009 Virgil Dobjanschi, Jeff Sharkey
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

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import no.java.schedule.R;
import no.java.schedule.activities.adapters.SessionsAdapter;
import no.java.schedule.activities.adapters.listitems.EmptyBlockListItem;
import no.java.schedule.activities.adapters.listitems.ListItem;
import no.java.schedule.activities.adapters.listitems.SessionListItem;
import no.java.schedule.activities.fullscreen.SessionDetailsActivity;
import no.java.schedule.provider.SessionsContract.BlocksColumns;
import no.java.schedule.provider.SessionsContract.Sessions;
import no.java.schedule.provider.SessionsContract.Tracks;

/**
 * An activity which displays a list
 */
public class SessionsListActivity extends ListActivity implements OnItemClickListener{

    // private static final String TAG = "SessionsListActivity";

    public static final String EXTRA_CHILD_MODE = "childmode";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_SELECTION_ARGS = "selection_args";

    public static final int CHILD_MODE_ALL = 0x0;
    public static final int CHILD_MODE_STARRED = 0x1;
    public static final int CHILD_MODE_VISIBLE_TRACKS = 0x2;
    public static final int CHILD_MODE_PICK = 0x3;
    private SessionsAdapter m_adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        final Intent intent = getIntent();
        int childMode = intent.getIntExtra(EXTRA_CHILD_MODE, CHILD_MODE_ALL);

        Uri uri = Sessions.CONTENT_URI;
        String selection = null;
        String[] selectionArgs = null;
        int mode = SessionsAdapter.MODE_SCHEDULE;
        switch( childMode) {
            case CHILD_MODE_STARRED:
                selection = Sessions.STARRED + "=?";
                selectionArgs = new String[]{"1"};
                mode = SessionsAdapter.MODE_STARRED;
                break;

            case CHILD_MODE_VISIBLE_TRACKS:
                selection = Tracks.VISIBLE + "=?";
                selectionArgs = new String[]{"1"};
                break;

            case CHILD_MODE_PICK:
                selection = intent.getStringExtra(EXTRA_SELECTION);
                selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
                break;
            default:
                break;
        }

        m_adapter = new SessionsAdapter( this, uri, selection, selectionArgs, null, mode);
        getListView().setAdapter( m_adapter);
        getListView().setOnItemClickListener( this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (m_adapter != null) {
            getListView().setAdapter(null);
            m_adapter.close();
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
        ListItem listItem = m_adapter.getItemByPosition(position);
        switch (listItem.getType()){

            case ListItem.TYPE_SESSION:
                startDetailActivityFor(listItem);
                return;

            case ListItem.TYPE_EMPTY_BLOCK:
                startSelectSessionActivity(listItem);
                return;

            default:
                return;
        }
    }

    private void startSelectSessionActivity(ListItem listItem) {
        EmptyBlockListItem eti = (EmptyBlockListItem) listItem;
        Intent intent = new Intent().setClass( this, SessionsListActivity.class);
        intent.setAction( Intent.ACTION_PICK);
        intent.putExtra(SessionsListActivity.EXTRA_CHILD_MODE,
                SessionsListActivity.CHILD_MODE_PICK);
        intent.putExtra( EXTRA_SELECTION, "(" + BlocksColumns.TIME_START + "=?) AND (" + BlocksColumns.TIME_END + "=?)");
        intent.putExtra( EXTRA_SELECTION_ARGS, new String[] { "" + eti.getStartTime(), "" + eti.getEndTime() });
        startActivityForResult( intent, 1);
    }

    private void startDetailActivityFor(ListItem listItem) {
        SessionListItem si = (SessionListItem) listItem;
        // Start details activity for selected listItem
        Intent intent = new Intent( this, SessionDetailsActivity.class);
        intent.setAction( Intent.ACTION_VIEW);
        intent.setData( si.getSessionItem().getUri());
        startActivityForResult( intent, 0);
    }
}
