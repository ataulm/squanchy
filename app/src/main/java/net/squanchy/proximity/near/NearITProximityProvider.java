package net.squanchy.proximity.near;

import net.squanchy.proximity.ProximityEvent;
import net.squanchy.proximity.ProximityProvider;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;
import it.near.sdk.NearItManager;
import it.near.sdk.geopolis.beacons.ranging.ProximityListener;
import it.near.sdk.reactions.customjson.CustomJSON;

public class NearITProximityProvider implements ProximityProvider {

    private final NearItManager nearItManager;
    private static final String PROXIMITY_ACTION = "action";
    private static final String SUBJECT_ID = "subject";

    public NearITProximityProvider(NearItManager nearItManager) {
        this.nearItManager = nearItManager;
        // TODO set a notification icon if needed
    }

    @Override
    public void startRadar() {
        nearItManager.startRadar();
    }

    @Override
    public void stopRadar() {
        nearItManager.stopRadar();
    }

    @Override
    public Observable<ProximityEvent> observeProximityEvents() {
        return Observable.create((ObservableEmitter<ProximityEvent> e) -> {
            ProximityListener listener = (parcelable, recipe) -> {
                if (parcelable instanceof CustomJSON) {
                    CustomJSON customJSON = (CustomJSON) parcelable;
                    String action = (String) customJSON.getContent().get(PROXIMITY_ACTION);
                    String subject = (String) customJSON.getContent().get(SUBJECT_ID);
                    if (action != null && subject != null) {
                        e.onNext(ProximityEvent.create(action, subject));
                    }
                }
            };
            nearItManager.addProximityListener(listener);

            e.setCancellable(() -> nearItManager.removeProximityListener(listener));
        }).observeOn(Schedulers.io());
    }
}
