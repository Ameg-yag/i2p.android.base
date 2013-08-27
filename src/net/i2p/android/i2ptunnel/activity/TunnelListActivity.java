package net.i2p.android.i2ptunnel.activity;

import net.i2p.android.i2ptunnel.fragment.TunnelDetailFragment;
import net.i2p.android.i2ptunnel.fragment.TunnelListFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.I2PActivityBase;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class TunnelListActivity extends I2PActivityBase implements
        TunnelListFragment.OnTunnelSelectedListener,
        TunnelDetailFragment.OnTunnelDeletedListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected boolean canUseTwoPanes() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Client tunnels tab
        TunnelListFragment cf = new TunnelListFragment();
        Bundle args = new Bundle();
        args.putBoolean(TunnelListFragment.SHOW_CLIENT_TUNNELS, true);
        cf.setArguments(args);
        Tab tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_client)
                .setTabListener(new TabListener(cf));
        actionBar.addTab(tab);

        // Server tunnels tab
        TunnelListFragment sf = new TunnelListFragment();
        args = new Bundle();
        args.putBoolean(TunnelListFragment.SHOW_CLIENT_TUNNELS, false);
        sf.setArguments(args);
        tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_server)
                .setTabListener(new TabListener(sf));
        actionBar.addTab(tab);

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            cf.setActivateOnItemClick(true);
            sf.setActivateOnItemClick(true);
        }
    }

    // TunnelListFragment.OnTunnelSelectedListener

    public void onTunnelSelected(int tunnelId) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, detailFrag).commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, TunnelDetailActivity.class);
            detailIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);
            startActivity(detailIntent);
        }
    }

    // TunnelDetailFragment.OnTunnelDeletedListener

    public void onTunnelDeleted() {
        getSupportFragmentManager().popBackStack();
    }
}
