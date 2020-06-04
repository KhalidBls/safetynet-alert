package com.safetynet.alerts.controllers;



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.safetynet.alerts.models.*;
import com.safetynet.alerts.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlertsController {

	@Autowired
	private EntitiesRepository repo;
	@Autowired
	private ChildrenService childrenService;
	@Autowired
	private FireService fireService;
	@Autowired
	private ZoneService zoneService;

	@Autowired
	private FirestationService firestationService;


	@GetMapping("/personInfo")
	public MappingJacksonValue afficherLesPersonne(@RequestParam(name="firstName", required = true)String firstName
			,@RequestParam(name="lastName", required = true)String lastName) throws Exception {
		List<Person> ourPersonList = new ArrayList<>();
		for(int i = 0;i<repo.getPersons().size(); i++) {
			if( repo.getPersons().get(i).getLastName().equals(lastName) )
				ourPersonList.add(repo.getPersons().get(i));
		}
		SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("firstName","city","zip","phone","birthdate");

		FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
		MappingJacksonValue personsFiltres = new MappingJacksonValue(ourPersonList);
		personsFiltres.setFilters(listDeNosFiltres);

		return personsFiltres;
	}

	
	@GetMapping("/firestation")
	public MappingJacksonValue afficherPersonnesDeZone(@RequestParam(name="stationNumber", required = true)String number) throws Exception {

		Firestation ourFirestation = firestationService.findByNumber(number);

		zoneService.setPersons(repo.getPersons()
				  .stream()
				  .filter(c -> c.getAddress().equals(ourFirestation.getAddress()))
				  .collect(Collectors.toList()));


		for (Person person : zoneService.getPersons()) {
			if(person.getAge()>=18)
				zoneService.increaseAdult();
			else
				zoneService.increaseChild();
		}

		SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("city","zip","email"
				,"birthdate","medications","allergies");
		FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
		MappingJacksonValue personFiltres = new MappingJacksonValue(zoneService);
		personFiltres.setFilters(listDeNosFiltres);

		return  personFiltres;
	}
	
	
	@GetMapping("/childAlert")
	public MappingJacksonValue afficherEnfant(@RequestParam(name="address", required = true)String address) {
		

		childrenService.setChildrens(repo.getPersons()
				.stream()
				.filter(c -> c.getAge() < 18 && c.getAddress().equals(address))
				.collect(Collectors.toList()));
		
		childrenService.setPersonFamily(repo.getPersons()
				.stream()
				.filter(c -> c.getAge() > 18 && c.getAddress().equals(address))
				.collect(Collectors.toList()));

		if(childrenService.getChildrens().size() == 0)
			return null;

		SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("address","city","zip","email","phone"
				,"birthdate","medications","allergies");
		FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
		MappingJacksonValue personFiltres = new MappingJacksonValue(childrenService);
		personFiltres.setFilters(listDeNosFiltres);

		return personFiltres;
	}

	@GetMapping("/fire")
	public MappingJacksonValue afficherHabitants(@RequestParam(name="address", required = true)String address) {

		Firestation ourFirestation = firestationService.findAll(address);

		fireService.setPersons( repo.getPersons()
				.stream()
				.filter(c -> c.getAddress().equals(address))
				.collect(Collectors.toList()));

		if (ourFirestation!=null)
			fireService.setFirestation(ourFirestation.getStation());

		SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("firstName","address","city","zip","email"
				,"birthdate");
		FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
		MappingJacksonValue personFiltres = new MappingJacksonValue(fireService);
		personFiltres.setFilters(listDeNosFiltres);


		return personFiltres;
	}
	
	@GetMapping("/phoneAlert")
	public List<String> afficherNumberByFirestation(@RequestParam(name="firestation", required = true)String firestation) {
		List<String> phoneNumber = new ArrayList<String>();

		Firestation ourFirestation = firestationService.findByNumber(firestation);

		List<Person> localPerson = repo.getPersons()
				.stream()
				.filter(c -> c.getAddress().equals(ourFirestation.getAddress()))
				.collect(Collectors.toList());

		for (Person person : localPerson) {
			phoneNumber.add(person.getPhone());
		}
		return phoneNumber;
		
	}


	@GetMapping("/communityEmail")
	public List<String> afficherEmailOfCity(@RequestParam(name="city", required = true)String city) {
		List<String> emailCommunity = new ArrayList<>();
		List<Person> personFromCity = repo.getPersons()
				.stream()
				.filter(c -> c.getCity().equals(city))
				.collect(Collectors.toList());

		for (Person person : personFromCity) {
			emailCommunity.add(person.getEmail());
		}
		return emailCommunity;
	}

	
}
