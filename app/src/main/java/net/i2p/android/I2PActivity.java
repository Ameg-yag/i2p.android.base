package net.i2p.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import net.i2p.android.help.HelpActivity;
import net.i2p.android.i2ptunnel.TunnelsContainer;
import net.i2p.android.router.ConsoleContainer;
import net.i2p.android.router.MainFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.router.addressbook.AddressbookContainer;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Connectivity;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.MemoryFragmentPagerAdapter;
import net.i2p.android.widget.SlidingTabLayout;

import java.io.File;

/**
 * The main activity of the app. Contains a ViewPager that holds the three main
 * views:
 * <ul>
 * <li>The console</li>
 * <li>The addressbook</li>
 * <li>The tunnel manager</li>
 * </ul>
 */
public class I2PActivity extends I2PActivityBase implements
        MainFragment.RouterControlListener {
    ViewPager mViewPager;
    ViewPagerAdapter mViewPagerAdapter;

    private boolean mAutoStartFromIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        // Center the tabs in the layout
        slidingTabLayout.setDistributeEvenly(true);
        // Customize tab color
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }
        });
        // Give the SlidingTabLayout the ViewPager
        slidingTabLayout.setViewPager(mViewPager);
    }

    public static class ViewPagerAdapter extends MemoryFragmentPagerAdapter {
        private static final int NUM_ITEMS = 3;

        private Context mContext;

        public ViewPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ConsoleContainer();
                case 1:
                    return new TunnelsContainer();
                case 2:
                    return new AddressbookContainer();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mContext.getString(R.string.label_console);
                case 1:
                    return mContext.getString(R.string.label_tunnels);
                case 2:
                    return mContext.getString(R.string.label_addresses);
                default:
                    return null;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Util.d("Initializing...");
        InitActivities init = new InitActivities(this);
        init.debugStuff();
        init.initialize();
        super.onPostCreate(savedInstanceState);
        handleIntents();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntents();
    }

    private void handleIntents() {
        if (getIntent() == null)
            return;

        Intent intent = getIntent();
        String action = intent.getAction();

        if (action == null)
            return;

        if (action.equals("net.i2p.android.router.START_I2P")) {
            if (mViewPager.getCurrentItem() != 0)
                mViewPager.setCurrentItem(0, false);
            autoStart();
        }
    }

    private void autoStart() {
        if (canStart()) {
            if (Connectivity.isConnected(this)) {
                mAutoStartFromIntent = true;
                onStartRouterClicked();
            } else {
                // Not connected to a network
                // TODO: Notify user
            }
        } else {
            // TODO: Notify user
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_CHANGED);
        lbm.registerReceiver(onStateChange, filter);
    }

    private BroadcastReceiver onStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            State state = intent.getParcelableExtra(RouterService.LOCAL_BROADCAST_EXTRA_STATE);
            if (state == State.RUNNING && mAutoStartFromIntent) {
                I2PActivity.this.setResult(RESULT_OK);
                finish();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_base_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_help:
                Intent hi = new Intent(this, HelpActivity.class);
                switch (mViewPager.getCurrentItem()) {
                    case 1:
                        hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_ADDRESSBOOK);
                        break;
                    case 2:
                        hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_I2PTUNNEL);
                        break;
                }
                startActivity(hi);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onStateChange);
    }

    private boolean canStart() {
        RouterService svc = _routerService;
        return (svc == null) || (!_isBound) || svc.canManualStart();
    }

    private boolean canStop() {
        RouterService svc = _routerService;
        return svc != null && _isBound && svc.canManualStop();
    }

    // MainFragment.RouterControlListener

    public boolean shouldShowOnOff() {
        return (canStart() && Connectivity.isConnected(this)) || canStop();
    }

    public boolean shouldBeOn() {
        String action = getIntent().getAction();
        return (canStop()) ||
                (action != null && action.equals("net.i2p.android.router.START_I2P"));
    }

    public void onStartRouterClicked() {
        RouterService svc = _routerService;
        if (svc != null && _isBound) {
            setPref(PREF_AUTO_START, true);
            svc.manualStart();
        } else {
            (new File(Util.getFileDir(this), "wrapper.log")).delete();
            startRouter();
        }
    }

    public boolean onStopRouterClicked() {
        RouterService svc = _routerService;
        if (svc != null && _isBound) {
            setPref(PREF_AUTO_START, false);
            svc.manualQuit();
            return true;
        }
        return false;
    }
}