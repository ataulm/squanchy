package net.squanchy.favorites;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import net.squanchy.R;
import net.squanchy.analytics.Analytics;
import net.squanchy.analytics.ContentType;
import net.squanchy.favorites.domain.view.Favorites;
import net.squanchy.navigation.Navigator;
import net.squanchy.proximity.ProximityEvent;
import net.squanchy.schedule.domain.view.Event;
import net.squanchy.schedule.view.ScheduleDayPageView;
import net.squanchy.schedule.view.ScheduleViewPagerAdapter;
import net.squanchy.service.proximity.injection.ProximityService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class FavoritesPageView extends CoordinatorLayout {

    private View progressBar;
    private CompositeDisposable subscriptions;
    private FavoritesService service;
    private Navigator navigate;
    private ProximityService proximityService;
    private Analytics analytics;
    private ScheduleDayPageView favoritesListView;

    public FavoritesPageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FavoritesPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Activity activity = unwrapToActivityContext(getContext());
        FavoritesComponent component = FavoritesInjector.obtain(activity);
        service = component.service();
        navigate = component.navigator();
        analytics = component.analytics();
        proximityService = component.proxService();

        progressBar = findViewById(R.id.progressbar);

        favoritesListView = (ScheduleDayPageView) findViewById(R.id.favorites_list);

        setupToolbar();
    }

    private static Activity unwrapToActivityContext(Context context) {
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            ContextWrapper contextWrapper = (ContextWrapper) context;
            return unwrapToActivityContext(contextWrapper.getBaseContext());
        } else {
            throw new IllegalStateException("Context type not supported: " + context.getClass().getCanonicalName());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.activity_favorites);
        toolbar.inflateMenu(R.menu.search_icon);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                navigate.toSearch();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscriptions = new CompositeDisposable();
        subscriptions.add(
                service.favorites()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(favorites -> updateWith(favorites, this::onEventClicked)));

        subscriptions.add(
                proximityService.observeProximityEvents()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleProximityEvent));
    }

    private void onEventClicked(Event event) {
        analytics.trackItemSelected(ContentType.SCHEDULE_ITEM, event.id());
        navigate.toEventDetails(event.id());
    }

    private void handleProximityEvent(ProximityEvent proximityEvent) {
        // TODO do something with the event, like showing feedback or opening an event detail
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscriptions.dispose();
    }

    public void updateWith(Favorites favorites, ScheduleViewPagerAdapter.OnEventClickedListener listener) {
        favoritesListView.updateWith(favorites.events(), listener);
        progressBar.setVisibility(GONE);
    }
}