package net.squanchy.service.repository;

import net.squanchy.venue.domain.view.Venue;
import net.squanchy.service.firebase.FirebaseDbService;

import io.reactivex.Observable;

public class VenueRepository {

    private final FirebaseDbService dbService;

    public VenueRepository(FirebaseDbService dbService) {
        this.dbService = dbService;
    }

    public Observable<Venue> venue() {
        return dbService.venueInfo()
                .map(firebaseVenue -> Venue.create(
                        firebaseVenue.name,
                        firebaseVenue.address,
                        firebaseVenue.lat,
                        firebaseVenue.lon,
                        firebaseVenue.description,
                        firebaseVenue.map_url
                ));
    }
}
