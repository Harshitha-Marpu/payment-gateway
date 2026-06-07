package com.payment.gateway.exception;

public class TransactionNotFoundException extends RuntimeException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}




// This exception is thrown when a transaction with a specified ID cannot be found in the database. It extends RuntimeException, allowing it to be used without mandatory try-catch blocks. The exception includes the transaction ID for easier debugging and logging.
// To use this exception, simply throw it in your service or controller when a transaction lookup fails. For example:
// Transaction transaction = transactionRepository.findById(transactionId)
//     .orElseThrow(() -> new TransactionNotFoundException(transactionId));
// You can also create a global exception handler to catch this exception and return a user-friendly error response to the client. For example:
// @ControllerAdvice
// public class GlobalExceptionHandler {
//     @ExceptionHandler(TransactionNotFoundException.class)
//     public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
//         ErrorResponse error = new ErrorResponse("Transaction Not Found", ex.getMessage());
//         return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
//     }
// }
// End of TransactionNotFoundException.java.
// Note: In a real application, you would likely want to include additional information in the error response, such as a timestamp, error code, or more detailed message. The above example is simplified for demonstration purposes.
// End of TransactionNotFoundException.java --- IGNORE ---
// This file defines a custom exception called TransactionNotFoundException, which is used to indicate that a transaction with a specified ID could not be found in the database. It includes the transaction ID for easier debugging and can be caught and handled in a global exception handler to return user-friendly error responses to clients.
// Remember to handle exceptions carefully in a payment gateway context, as they can contain sensitive information. Always ensure that error messages do not expose sensitive data and that logging is done securely.
// End of TransactionNotFoundException.java.
















