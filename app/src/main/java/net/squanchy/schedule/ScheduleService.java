package net.squanchy.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.squanchy.schedule.domain.view.Event;
import net.squanchy.schedule.domain.view.Schedule;
import net.squanchy.schedule.domain.view.SchedulePage;
import net.squanchy.service.firebase.FirebaseAuthService;
import net.squanchy.service.firebase.FirebaseDbService;
import net.squanchy.service.firebase.model.FirebaseDays;
import net.squanchy.service.repository.EventRepository;
import net.squanchy.support.lang.Func2;
import net.squanchy.support.lang.Lists;
import net.squanchy.support.lang.Optional;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static net.squanchy.support.lang.Lists.filter;
import static net.squanchy.support.lang.Lists.find;

public class ScheduleService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    private final FirebaseDbService dbService;
    private final FirebaseAuthService authService;
    private final EventRepository eventRepository;

    public ScheduleService(FirebaseDbService dbService, FirebaseAuthService authService, EventRepository eventRepository) {
        this.dbService = dbService;
        this.authService = authService;
        this.eventRepository = eventRepository;
    }

    public Observable<Schedule> schedule(boolean onlyFavorites) {
        return authService.ifUserSignedInThenObservableFrom(userId -> {
            Observable<FirebaseDays> daysObservable = dbService.days();

            return eventRepository.events(userId)
                    .map(events -> onlyFavorites ? filter(events, Event::favorited) : events)
                    .map(groupEventsByDay())
                    .withLatestFrom(daysObservable, combineEventsById())
                    .map(sortPagesByDate())
                    .map(sortEventsByStartDate())
                    .subscribeOn(Schedulers.io());
        });
    }

    private Function<List<Event>, HashMap<String, List<Event>>> groupEventsByDay() {
        return events -> Lists.reduce(new HashMap<>(), events, listToDaysHashMap());
    }

    private Func2<HashMap<String, List<Event>>, Event, HashMap<String, List<Event>>> listToDaysHashMap() {
        return (map, event) -> {
            List<Event> dayList = getOrCreateDayList(map, event);
            dayList.add(event);
            map.put(event.dayId(), dayList);
            return map;
        };
    }

    private List<Event> getOrCreateDayList(HashMap<String, List<Event>> map, Event event) {
        List<Event> currentList = map.get(event.dayId());

        if (currentList == null) {
            currentList = new ArrayList<>();
            map.put(event.dayId(), currentList);
        }

        return currentList;
    }

    private BiFunction<HashMap<String, List<Event>>, FirebaseDays, Schedule> combineEventsById() {
        return (map, apiDays) -> {
            List<SchedulePage> pages = new ArrayList<>(map.size());
            for (String dayId : map.keySet()) {
                Optional<String> rawDate = findDate(apiDays, dayId);
                if (rawDate.isPresent()) {
                    LocalDateTime date = LocalDateTime.parse(rawDate.get(), DATE_FORMATTER);
                    pages.add(SchedulePage.create(dayId, date, map.get(dayId)));
                }
            }

            return Schedule.create(pages);
        };
    }

    private Optional<String> findDate(FirebaseDays apiDays, String dayId) {
        return find(apiDays.days, firebaseDay -> firebaseDay.id.equals(String.valueOf(dayId)))
                .map(firebaseDay -> firebaseDay.date);
    }

    private Function<Schedule, Schedule> sortPagesByDate() {
        return schedule -> {
            ArrayList<SchedulePage> sortedPages = new ArrayList<>(schedule.pages());
            Collections.sort(sortedPages, (firstPage, secondPage) -> firstPage.date().compareTo(secondPage.date()));
            return Schedule.create(sortedPages);
        };
    }

    private Function<Schedule, Schedule> sortEventsByStartDate() {
        return schedule -> {
            List<SchedulePage> pages = schedule.pages();
            List<SchedulePage> sortedPages = new ArrayList<>(pages.size());

            for (SchedulePage page : pages) {
                sortedPages.add(SchedulePage.create(page.dayId(), page.date(), sortByStartDate(page)));
            }

            return Schedule.create(sortedPages);
        };
    }

    private List<Event> sortByStartDate(SchedulePage schedulePage) {
        ArrayList<Event> sortedEvents = new ArrayList<>(schedulePage.events());
        Collections.sort(sortedEvents, (firstEvent, secondEvent) -> firstEvent.startTime().compareTo(secondEvent.startTime()));
        return sortedEvents;
    }
}
