package net.squanchy.eventdetails;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.squanchy.R;
import net.squanchy.eventdetails.EventDetailsService.FavoriteResult;
import net.squanchy.eventdetails.widget.EventDetailsCoordinatorLayout;
import net.squanchy.fonts.TypefaceStyleableActivity;
import net.squanchy.navigation.Navigator;
import net.squanchy.schedule.domain.view.Event;
import net.squanchy.speaker.domain.view.Speaker;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class EventDetailsActivity extends TypefaceStyleableActivity {

    private static final String EXTRA_EVENT_ID = EventDetailsActivity.class.getCanonicalName() + ".event_id";

    private EventDetailsService service;
    private CompositeDisposable subscriptions;
    private EventDetailsCoordinatorLayout coordinatorLayout;

    private Navigator navigator;

    public static Intent createIntent(Context context, String eventId) {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_details);

        setupToolbar();

        EventDetailsComponent component = EventDetailsInjector.obtain(this);
        service = component.service();
        navigator = component.navigator();

        coordinatorLayout = (EventDetailsCoordinatorLayout) findViewById(R.id.event_details_root);
        subscriptions = new CompositeDisposable();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        String eventId = intent.getStringExtra(EXTRA_EVENT_ID);

        subscriptions.add(service.event(eventId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> coordinatorLayout.updateWith(event, onEventDetailsClickListener(event))));
    }

    private EventDetailsCoordinatorLayout.OnEventDetailsClickListener onEventDetailsClickListener(Event event) {
        return new EventDetailsCoordinatorLayout.OnEventDetailsClickListener() {
            @Override
            public void onSpeakerClicked(Speaker speaker) {
                navigate().toSpeakerDetails(speaker.id());
            }

            @Override
            public void onFavoriteClick() {

                subscriptions.add(service.toggleFavorite(event).subscribe(result -> {
                    if (result == FavoriteResult.MUST_AUTHENTICATE) {
                        Toast.makeText(EventDetailsActivity.this, "You need to sign in for this", Toast.LENGTH_SHORT).show();
                        navigate().toSignIn(); // TODO UI stuff
                    }
                }));
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_icon, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            navigate().toSearch();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Navigator navigate() {
        return navigator;
    }

    @Override
    protected void onStop() {
        super.onStop();

        subscriptions.clear();
    }
}
