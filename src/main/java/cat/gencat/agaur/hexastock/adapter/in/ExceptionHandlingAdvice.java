package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.HoldingNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.ExternalApiException;
import cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException;
import cat.gencat.agaur.hexastock.model.exception.InvalidTickerException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.ProblemDetail;

@ControllerAdvice
public class ExceptionHandlingAdvice {

    @ExceptionHandler(PortfolioNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ProblemDetail portfolioNotFoundExceptionHandler(PortfolioNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Portfolio Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(HoldingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ProblemDetail holdingNotFoundExceptionHandler(HoldingNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Holding Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ProblemDetail invalidAmountExceptionHandler(InvalidAmountException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Amount");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ProblemDetail invalidQuantityExceptionHandler(InvalidQuantityException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Quantity");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConflictQuantityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    String conflictExceptionHandler(Exception ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    String insufficientFundsExceptionHandler(InsufficientFundsException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    String externalApiExceptionHandler(ExternalApiException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InvalidTickerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ProblemDetail invalidTickerExceptionHandler(InvalidTickerException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Ticker");
        pd.setDetail(ex.getMessage());
        return pd;
    }

}