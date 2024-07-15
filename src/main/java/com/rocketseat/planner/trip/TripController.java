package com.rocketseat.planner.trip;

import com.rocketseat.planner.activity.*;
import com.rocketseat.planner.link.*;
import com.rocketseat.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    // TRIPS
    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload){
        Trip newTrip = new Trip(payload);

        this.tripRepository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){
        Optional<Trip> trip = this.tripRepository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(()-> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if (trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());

            this.tripRepository.save(rawTrip);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);
            this.tripRepository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(rawTrip.getId());
            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    //PARTICIPANT

    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            if(rawTrip.getIsConfirmed()) this.participantService.triggerConfirmationEmailToParticipant(payload.email());

            return ResponseEntity.ok(participantResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipants(@PathVariable UUID id){
        List<ParticipantData> participantsList = this.participantService.getAllParticipantsFromTrip(id);

        return ResponseEntity.ok(participantsList);
    }

    //ACTIVITIES

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequestPayload payload){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            ActivityResponse activityResponse = this.activityService.registerActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id){
        List<ActivityData> activitiesList = this.activityService.getAllActivitiesFromId(id);

        return ResponseEntity.ok(activitiesList);
    }

    // LINKS

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequestPayload payload){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            LinkResponse linkResponse = this.linkService.registerLink(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id){
        List<LinkData> linkList = this.linkService.getAllLinksFromId(id);

        return ResponseEntity.ok(linkList);
    }

}
