package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.application.port.in.ConflictQuantityException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidAmountException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidQuantityException;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionHandlingAdvice {

    @ExceptionHandler(PortfolioNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    String notFoundExceptionHandler(Exception ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({InvalidAmountException.class, InvalidQuantityException.class, DomainException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    String badRequestExceptionHandler(Exception ex) { return ex.getMessage(); }

    @ExceptionHandler(ConflictQuantityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    String conflictExceptionHandler(Exception ex) {
        return ex.getMessage();
    }

}

