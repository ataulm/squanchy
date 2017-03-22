package net.squanchy.service.firebase.model;

import android.support.annotation.Nullable;

import java.util.List;

public class FirebaseEvent {

    public String id;

    public String name;

    public String type;

    public Long start_time;

    public Long end_time;

    public String day_id;

    public String description;

    @Nullable
    public String track_id;

    @Nullable
    public String experience_level;

    @Nullable
    public String place_id;

    @Nullable
    public List<String> speaker_ids;

    @Nullable
    public FirebaseLinks links;
}
