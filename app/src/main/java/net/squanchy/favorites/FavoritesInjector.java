package net.squanchy.favorites;

import android.app.Activity;

import net.squanchy.injection.ActivityContextModule;
import net.squanchy.injection.ApplicationInjector;
import net.squanchy.navigation.NavigationModule;
import net.squanchy.schedule.ScheduleModule;

final class FavoritesInjector {

    private FavoritesInjector() {
        // no instances
    }

    public static FavoritesComponent obtain(Activity activity) {
        return DaggerFavoritesComponent.builder()
                .applicationComponent(ApplicationInjector.obtain(activity))
                .scheduleModule(new ScheduleModule())
                .navigationModule(new NavigationModule())
                .activityContextModule(new ActivityContextModule(activity))
                .build();
    }
}
