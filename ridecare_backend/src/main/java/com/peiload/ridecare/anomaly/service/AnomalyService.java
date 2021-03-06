package com.peiload.ridecare.anomaly.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peiload.ridecare.anomaly.dto.AlternativeClassificationShowDto;
import com.peiload.ridecare.anomaly.dto.AnomalyShowDto;
import com.peiload.ridecare.anomaly.dto.DetailedAnomalyShowDto;
import com.peiload.ridecare.anomaly.dto.MeasurementSetDto;
import com.peiload.ridecare.anomaly.dto.MeasurementShowDto;
import com.peiload.ridecare.anomaly.model.Anomaly;
import com.peiload.ridecare.anomaly.model.Measurement;
import com.peiload.ridecare.anomaly.repository.AnomalyRepository;
import com.peiload.ridecare.anomaly.repository.MeasurementRepository;
import com.peiload.ridecare.car.model.Car;
import com.peiload.ridecare.car.service.CarService;
import com.peiload.ridecare.common.JwtTokenUtil;
import com.peiload.ridecare.user.model.User;
import com.peiload.ridecare.user.service.UserService;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final MeasurementRepository measurementRepository;
    private final JwtTokenUtil jtu;
    private final UserService userService;
    private final CarService carService;

    @Value("${ridecare.datalake.url}")
    public String datalakeURL;

    @Value("${ridecare.alertAI.url}")
    public String alertAIURL;

    public AnomalyService(AnomalyRepository anomalyRepository, MeasurementRepository measurementRepository, JwtTokenUtil jtu, UserService userService, CarService carService) {
        this.anomalyRepository = anomalyRepository;
        this.measurementRepository = measurementRepository;
        this.jtu = jtu;
        this.userService = userService;
        this.carService = carService;
    }

    public Anomaly findById(int anomalyId) {
        return this.anomalyRepository.findById(anomalyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anomaly does not exist."));
    }

    public List<AnomalyShowDto> getAllAnomalies() {
        List<AnomalyShowDto> anomalies = this.anomalyRepository.findAll().stream().map(AnomalyShowDto::new).collect(Collectors.toList());
        Collections.reverse(anomalies);
        return anomalies;
    }

    public List<AnomalyShowDto> getAllUserAnomalies(String authorizationToken){
        String email = jtu.getEmailFromAuthorizationString(authorizationToken);
        User user = userService.findByEmail(email);

        List<Car> userCars = user.getCars();
        List<AnomalyShowDto> anomalies = anomalyRepository.findAllByCarIn(userCars).stream().map(AnomalyShowDto::new).collect(Collectors.toList());
        Collections.reverse(anomalies);
        return anomalies;
    }

    public List<AnomalyShowDto> getLatestAnomaliesUser(String authorizationToken) {
        String email = jtu.getEmailFromAuthorizationString(authorizationToken);
        User user = userService.findByEmail(email);

        List<Car> userCars = user.getCars();

        List<AnomalyShowDto> anomalies = anomalyRepository.findAllByViewedAndCarIn(false, userCars);
        Collections.reverse(anomalies);
        return anomalies;
    }

    public List<AnomalyShowDto> getLatestAnomaliesCar(String authorizationToken, int carId) {
        String email = jtu.getEmailFromAuthorizationString(authorizationToken);
        Car car = this.carService.findById(carId);

        if(car.getUser().getEmail().equals(email)){
            List<AnomalyShowDto> anomalies = anomalyRepository.findAllByViewedAndCar(false, car);
            Collections.reverse(anomalies);
            return anomalies;
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The car you were trying to view belongs to another user");
        }

    }

    public List<AnomalyShowDto> getAnomaliesByDate(String authorizationToken, Date date) {
        String email = jtu.getEmailFromAuthorizationString(authorizationToken);
        User user = userService.findByEmail(email);

        List<Car> userCars = user.getCars();
        List<Anomaly> userAnomalies = anomalyRepository.findAllByCarIn(userCars);

       return userAnomalies.stream().filter(anomaly -> DateUtils.isSameDay(anomaly.getMeasurements().get(0).getDate(),date)
               || DateUtils.isSameDay(anomaly.getMeasurements().get(anomaly.getMeasurements().size()-1).getDate(),date)
            ).map(AnomalyShowDto::new).collect(Collectors.toList());
    }

    public List<AnomalyShowDto> getAnomaliesBetweenDates(String authorizationToken, Date initialDate, Date finalDate){
        String email = jtu.getEmailFromAuthorizationString(authorizationToken);
        User user = userService.findByEmail(email);

        List<Car> userCars = user.getCars();
        List<Anomaly> userAnomalies = anomalyRepository.findAllByCarIn(userCars);
        List<AnomalyShowDto> history = new ArrayList<>();

        for(int i = 0; i < userAnomalies.size(); i++){
            List<Measurement> measurements = userAnomalies.get(i).getMeasurements();
            //como basta uma data estar depois da initialDate, verifica a mais recente
            if(measurements.get(measurements.size()-1).getDate().after(initialDate)){
                //como basta uma data estar antes da finalDate, verifica a mais antiga
                if(measurements.get(0).getDate().before(finalDate)){
                    history.add(new AnomalyShowDto(userAnomalies.get(i)));
                }
                else{
                    return history;
                }
            }
        }

        return history;
    }

    public Optional<Anomaly> createAnomaly(int carId, MeasurementSetDto measurementSetDto) {
        Car car = this.carService.findById(carId);

        if(car.getAnomalies().isEmpty()){
            Anomaly newAnomaly = new Anomaly(measurementSetDto, car);
            this.anomalyRepository.save(newAnomaly);
            this.measurementRepository.save(new Measurement(measurementSetDto, newAnomaly));
            return Optional.of(newAnomaly);
        }
        else{
            List<Anomaly> anomalies = car.getAnomalies();
            Anomaly lastAnomaly = anomalies.get(anomalies.size()-1);

            List<Measurement> measurements = lastAnomaly.getMeasurements();
            Measurement lastMeasurement = measurements.get(measurements.size()-1);

            //If the new measurement exceeds 30 seconds timeout or if the classifications are different, creates a new anomaly
            if(lastMeasurement.getDate().toInstant().plusSeconds(30).isBefore(measurementSetDto.getDate().toInstant())
                    || !(measurementSetDto.getClassification().equals(lastAnomaly.getClassification()))){
                Anomaly newAnomaly = new Anomaly(measurementSetDto, car);
                this.anomalyRepository.save(newAnomaly);
                this.measurementRepository.save(new Measurement(measurementSetDto, newAnomaly));
                return Optional.of(newAnomaly);
            }
            else {
                this.measurementRepository.save(new Measurement(measurementSetDto, lastAnomaly));
                return Optional.empty();
            }
        }
    }

    public void setAnomalyAsViewed(int anomalyId) {
        Optional<Anomaly> existingAnomaly = this.anomalyRepository.findById(anomalyId);

        if(existingAnomaly.isPresent()){
            Anomaly anomaly = existingAnomaly.get();
            anomaly.setViewed(true);
            this.anomalyRepository.save(anomaly);
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anomaly doesn't exist");
        }
    }

    public List<MeasurementShowDto> getMeasurements(String path, String licensePlate, Date date, int numberOfMeasurements){

        long secs = (date.getTime())/1000;

        String url = datalakeURL + path + "/"+ licensePlate + "/" + secs + "/" + numberOfMeasurements;
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> rateResponse = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        ObjectMapper objectMapper = new ObjectMapper();
        List<MeasurementShowDto> measurements;
        try {
            measurements = objectMapper.readValue(rateResponse.getBody(), new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            measurements = new ArrayList<>();
            e.printStackTrace();
        }

        return measurements;
    }

    public DetailedAnomalyShowDto getDetailedAnomaly(int anomalyId, int numberOfMeasurements){
        Anomaly anomaly = this.findById(anomalyId);
        String licensePlate = anomaly.getCar().getLicensePlate();

        List<Measurement> measurements = anomaly.getMeasurements();
        Date beginning = Date.from(measurements.get(0).getDate().toInstant().minusSeconds(1));

        List<MeasurementShowDto> beforeAnomaly = getMeasurements("history", licensePlate, beginning, numberOfMeasurements);
        beforeAnomaly = beforeAnomaly.stream().filter(measurement -> measurement.getTimeValue().toInstant().isAfter(beginning.toInstant().minusMillis(300000))).collect(Collectors.toList());
        Collections.reverse(beforeAnomaly);

        Date end = Date.from(measurements.get(measurements.size()-1).getDate().toInstant().plusSeconds(1));
        List<MeasurementShowDto> afterAnomaly = getMeasurements("recent", licensePlate, end, numberOfMeasurements);
        afterAnomaly = afterAnomaly.stream().filter(measurement -> measurement.getTimeValue().toInstant().isBefore(end.toInstant().plusMillis(300000))).collect(Collectors.toList());

        String date = measurements.get(0).getDate().toString().split("\\.")[0];

        String url = alertAIURL + "/models?licensePlate=" + licensePlate + "&timeValue=" + date;
        RestTemplate restTemplate = new RestTemplate();

        ObjectMapper objectMapper = new ObjectMapper();
        List<AlternativeClassificationShowDto> alternativeClassification;

        try {
            ResponseEntity<String> rateResponse = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            alternativeClassification = objectMapper.readValue(rateResponse.getBody(), new TypeReference<>(){});
        } catch (Exception e) {
            alternativeClassification = new ArrayList<>();
            e.printStackTrace();
        }

        return new DetailedAnomalyShowDto(anomaly, alternativeClassification, beforeAnomaly, afterAnomaly);
    }
}
