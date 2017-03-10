package com.sap.bulletinboard.ads.controllers;

import static org.springframework.http.HttpStatus.*;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.sap.bulletinboard.ads.models.Advertisement;
import com.sap.bulletinboard.ads.models.AdvertisementRepository;

/*
 * Use a path which does not end with a slash! Otherwise the controller is not reachable when not using the trailing
 * slash in the URL
 */
@RestController
@RequestMapping(path = AdvertisementController.PATH)
@RequestScope // @Scope(WebApplicationContext.SCOPE_REQUEST)
@Validated
public class AdvertisementController {
    public static final String PATH = "/api/v1.0/ads";
    public static final String DEFAULT_PAGE_ID = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";

    private AdvertisementRepository adRepository;
    
    @Inject
    public AdvertisementController(AdvertisementRepository repository) {
        this.adRepository = repository;
    }

    @GetMapping
    public Iterable<Advertisement> advertisements(
            @RequestParam(name = "pageId", defaultValue = DEFAULT_PAGE_ID) int pageId, 
            @RequestParam(name = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return adRepository.findAll(new PageRequest(pageId, pageSize));
    }

    @GetMapping("/{id}")
    public Advertisement advertisementById(@PathVariable("id") @Min(0) Long id) {
        throwIfNonexisting(id);
        return adRepository.findOne(id);
    }

    /**
     * @RequestBody is bound to the method argument. HttpMessageConverter resolves method argument depending on the
     *              content type.
     */
    @PostMapping
    public ResponseEntity<Advertisement> add(@Valid @RequestBody Advertisement advertisement,
            UriComponentsBuilder uriComponentsBuilder) throws URISyntaxException {

        Advertisement savedAdvertisement = adRepository.save(advertisement);

        UriComponents uriComponents = uriComponentsBuilder.path(PATH + "/{id}").buildAndExpand(savedAdvertisement.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(uriComponents.getPath()));
        return new ResponseEntity<>(savedAdvertisement, headers, HttpStatus.CREATED);
    }

    @DeleteMapping
    @ResponseStatus(NO_CONTENT)
    public void deleteAll() {
        adRepository.deleteAll();
    }

    @DeleteMapping("{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteById(@PathVariable("id") Long id) {
        throwIfNonexisting(id);
        adRepository.delete(id);
    }

    @PutMapping("/{id}")
    public Advertisement update(@PathVariable("id") long id, @RequestBody Advertisement updatedAd) {
        throwIfInconsistent(id, updatedAd.getId());
        throwIfNonexisting(id);
        return adRepository.save(updatedAd);
    }

    private void throwIfNonexisting(long id) {
        if (!adRepository.exists(id)) {
            throw new NotFoundException(id + " not found");
        }
    }
    
    private void throwIfInconsistent(Long expected, Long actual) {
        if (!expected.equals(actual)) {
            String message = String.format(
                    "bad request, inconsistent IDs between request and object: request id = %d, object id = %d",
                    expected, actual);
            throw new BadRequestException(message);
        }
    }
}
