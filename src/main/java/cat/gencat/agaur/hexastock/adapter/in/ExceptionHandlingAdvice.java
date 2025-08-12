package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.HoldingNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.ExternalApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionHandlingAdvice {

    @ExceptionHandler({PortfolioNotFoundException.class, HoldingNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    String notFoundExceptionHandler(Exception ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({InvalidAmountException.class, InvalidQuantityException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    String badRequestExceptionHandler(Exception ex) { return ex.getMessage(); }

    @ExceptionHandler(ConflictQuantityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    String conflictExceptionHandler(Exception ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    String externalApiExceptionHandler(ExternalApiException ex) {
        return ex.getMessage();
    }

}