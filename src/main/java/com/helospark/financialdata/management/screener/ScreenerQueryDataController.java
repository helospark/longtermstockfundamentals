package com.helospark.financialdata.management.screener;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.screener.repository.Screener;
import com.helospark.financialdata.management.screener.repository.ScreenerRepository;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.watchlist.repository.MessageCompresser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/screener-query")
public class ScreenerQueryDataController {
    private static final int MAX_NUMBER_OF_SAVED_SCREENERS = 50;
    @Autowired
    private ScreenerController screenerController;
    @Autowired
    private MessageCompresser messageCompresser;
    @Autowired
    private ScreenerRepository screenerRepository;
    @Autowired
    private LoginController loginController;

    @PostMapping
    public void saveScreenerQuery(@RequestBody ScreenerQuery query, HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (!jwt.isPresent()) {
            throw new ScreenerClientSideException("This feature require login");
        }

        screenerController.validateRequest(query.operations, query.exchanges, httpRequest);
        if (query.id == null) {
            throw new ScreenerClientSideException("Screener name is mandatory");
        }
        if (query.id.length() > 50) {
            throw new ScreenerClientSideException("Maximum screener name is 50 characters");
        }

        String email = jwt.get().getSubject();

        Optional<Screener> optionalScreeners = screenerRepository.readScreenerByEmail(email);

        Screener screener;
        if (!optionalScreeners.isPresent()) {
            screener = new Screener();
            screener.setEmail(email);
        } else {
            screener = optionalScreeners.get();
        }

        List<ScreenerQuery> listOfScreeners = messageCompresser.uncompressListOf(screener.getScreenerRaw(), ScreenerQuery.class);

        if (listOfScreeners.size() >= MAX_NUMBER_OF_SAVED_SCREENERS) {
            throw new ScreenerClientSideException("Maximum number of screeners is " + MAX_NUMBER_OF_SAVED_SCREENERS);
        }

        listOfScreeners.removeIf(screenerIt -> screenerIt.id.equals(query.id));
        listOfScreeners.add(query);

        ByteBuffer compressedValue = messageCompresser.createCompressedValue(listOfScreeners);
        screener.setScreenerRaw(compressedValue);

        screenerRepository.save(screener);
    }

    @DeleteMapping
    public void deleteScreenerQuery(@RequestParam @NotNull String queryId, HttpServletRequest httpRequest) {
        String email = ensuredLoggedInAndGetEmail(httpRequest);
        if (queryId == null) {
            throw new ScreenerClientSideException("Screener name is mandatory");
        }
        if (queryId.length() > 50) {
            throw new ScreenerClientSideException("Maximum screener name is 50 characters");
        }

        Optional<Screener> optionalScreeners = screenerRepository.readScreenerByEmail(email);

        if (optionalScreeners.isEmpty()) {
            return;
        }
        Screener screener = optionalScreeners.get();

        List<ScreenerQuery> listOfScreeners = messageCompresser.uncompressListOf(screener.getScreenerRaw(), ScreenerQuery.class);

        int previousSize = listOfScreeners.size();
        listOfScreeners.removeIf(screenerIt -> screenerIt.id.equals(queryId));

        if (listOfScreeners.size() < previousSize) {
            ByteBuffer compressedValue = messageCompresser.createCompressedValue(listOfScreeners);
            screener.setScreenerRaw(compressedValue);

            screenerRepository.save(screener);
        }

    }

    @GetMapping
    public List<ScreenerQuery> getScreeners(HttpServletRequest httpRequest) {
        String email = ensuredLoggedInAndGetEmail(httpRequest);

        Optional<Screener> optionalScreeners = screenerRepository.readScreenerByEmail(email);

        if (optionalScreeners.isEmpty()) {
            return List.of();
        }
        Screener screener = optionalScreeners.get();

        List<ScreenerQuery> listOfScreeners = messageCompresser.uncompressListOf(screener.getScreenerRaw(), ScreenerQuery.class);

        return listOfScreeners;
    }

    public String ensuredLoggedInAndGetEmail(HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (!jwt.isPresent()) {
            throw new ScreenerClientSideException("This feature require login");
        }
        String email = jwt.get().getSubject();
        return email;
    }

}
