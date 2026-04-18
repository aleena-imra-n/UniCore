package model;

import java.time.LocalDate;

/**
 * Represents one row from FEE_RECORDS joined with FEE_CHALLANS.
 * Used by DAO → Service → UI.
 */
public class FeeRecord {

    private final int        feeId;
    private final String     semesterName;    // e.g. "Fall 2025"
    private final double     totalAmount;
    private final double     paidAmount;
    private final LocalDate  dueDate;
    private final String     status;          // unpaid | partial | paid

    // Challan info — may be null if no challan generated yet
    private final String     challanNumber;
    private final LocalDate  generatedDate;
    private final LocalDate  expiryDate;
    private final boolean    challanPaid;
    private final int        semesterId;

    public FeeRecord(int feeId, int semesterId, String semesterName,
                     double totalAmount, double paidAmount,
                     LocalDate dueDate, String status,
                     String challanNumber, LocalDate generatedDate,
                     LocalDate expiryDate, boolean challanPaid) {
        this.feeId         = feeId;
        this.semesterId    = semesterId;
        this.semesterName  = semesterName;
        this.totalAmount   = totalAmount;
        this.paidAmount    = paidAmount;
        this.dueDate       = dueDate;
        this.status        = status;
        this.challanNumber = challanNumber;
        this.generatedDate = generatedDate;
        this.expiryDate    = expiryDate;
        this.challanPaid   = challanPaid;
    }

    public int        getFeeId()         { return feeId; }
    public int        getSemesterId()    { return semesterId; }
    public String     getSemesterName()  { return semesterName; }
    public double     getTotalAmount()   { return totalAmount; }
    public double     getPaidAmount()    { return paidAmount; }
    public double     getOutstanding()   { return totalAmount - paidAmount; }
    public LocalDate  getDueDate()       { return dueDate; }
    public String     getStatus()        { return status; }
    public String     getChallanNumber() { return challanNumber; }
    public LocalDate  getGeneratedDate() { return generatedDate; }
    public LocalDate  getExpiryDate()    { return expiryDate; }
    public boolean    isChallanPaid()    { return challanPaid; }
    public boolean    hasChallan()       { return challanNumber != null; }
}