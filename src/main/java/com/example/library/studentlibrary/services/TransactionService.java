package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.library.studentlibrary.models.CardStatus.DEACTIVATED;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        Transaction transaction = new Transaction();
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        if(!bookRepository5.existsById(bookId) || bookRepository5.findById(bookId).get().isAvailable()==false) {
            transaction.setTransactionId(transaction.getTransactionId());
            transaction.setFineAmount(0);
            transaction.setIssueOperation(false);
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setTransactionDate(new Date());
            transaction.setCard(cardRepository5.findById(cardId).get());
            transactionRepository5.save(transaction);
            throw new Exception("Book is either unavailable or not present");
        }
        Book book = bookRepository5.findById(bookId).get();
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        Card card=cardRepository5.findById(cardId).get();
        if(card.getCardStatus().equals(DEACTIVATED)) {
            transaction.setTransactionId(transaction.getTransactionId());
            transaction.setFineAmount(0);
            transaction.setIssueOperation(false);
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setTransactionDate(new Date());
            transaction.setBook(bookRepository5.findById(bookId).get());
            transactionRepository5.save(transaction);
            throw new Exception("Card is invalid");
        }
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books

        List<Book> list  = card.getBooks();
        if(list.size()==max_allowed_books) {
            transaction.setTransactionId(transaction.getTransactionId());
            transaction.setFineAmount(0);
            transaction.setIssueOperation(false);
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setTransactionDate(new Date());
            transaction.setBook(bookRepository5.findById(bookId).get());
            transaction.setCard(cardRepository5.findById(cardId).get());
            transactionRepository5.save(transaction);
            throw new Exception("Book limit has reached for this card");
        }
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id
        book.setAvailable(false);
        list.add(book);
        card.setBooks(list);
        book.setCard(card);

        transaction.setTransactionId(transaction.getTransactionId());
        transaction.setFineAmount(0);
        transaction.setIssueOperation(true);
        transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
        transaction.setTransactionDate(new Date());
        transaction.setBook(bookRepository5.findById(bookId).get());
        transaction.setCard(cardRepository5.findById(cardId).get());
        transactionRepository5.save(transaction);
        //Note that the error message should match exactly in all cases

       return transaction.getTransactionId(); //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);
        Book book=transaction.getBook();
        book.setAvailable(true);
        Date issueDate=transaction.getTransactionDate();
        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        // to calculate time from issue date till now in milliseconds
        long timeIssueTime = Math.abs(System.currentTimeMillis() - issueDate.getTime());

        // calculate above milliseconds into days
        long no_of_days_passed = TimeUnit.DAYS.convert(timeIssueTime, TimeUnit.MILLISECONDS);

        int fine = 0;
        if(no_of_days_passed > getMax_allowed_days) {
            fine = (int)((no_of_days_passed - getMax_allowed_days) * fine_per_day);
        }


        book.setAvailable(true);
        book.setCard(null);

        bookRepository5.updateBook(book);

        Transaction tr = new Transaction();
        tr.setBook(transaction.getBook());
        tr.setCard(transaction.getCard());
        tr.setIssueOperation(false);
        tr.setFineAmount(fine);
        tr.setTransactionStatus(TransactionStatus.SUCCESSFUL);

        transactionRepository5.save(tr);

        return tr; //return the transaction after updating all details
    }
}