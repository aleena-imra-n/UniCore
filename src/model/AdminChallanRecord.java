package model;

import java.time.LocalDate;

/**
 * Represents one challan row as seen by the Admin fee tracking panel.
 * Includes student info, semester, fee amounts, and challan status.
 */
public class AdminChallanRecord {

    private final int       challanId;
    private final int       feeId;
    private final String    challanNumber;
    private final String    studentName;
    private final String    rollNumber;
    private final String    semesterName;
    private final double    totalAmount;
    private final double    paidAmount;
    private final LocalDate generatedDate;
    private final LocalDate expiryDate;
    private final boolean   isPaid;

    public AdminChallanRecord(int challanId, int feeId,
                               String challanNumber,
                               String studentName, String rollNumber,
                               String semesterName,
                               double totalAmount, double paidAmount,
                               LocalDate generatedDate, LocalDate expiryDate,
                               boolean isPaid) {
        this.challanId     = challanId;
        this.feeId         = feeId;
        this.challanNumber = challanNumber;
        this.studentName   = studentName;
        this.rollNumber    = rollNumber;
        this.semesterName  = semesterName;
        this.totalAmount   = totalAmount;
        this.paidAmount    = paidAmount;
        this.generatedDate = generatedDate;
        this.expiryDate    = expiryDate;
        this.isPaid        = isPaid;
    }

    public int       getChallanId()     { return challanId; }
    public int       getFeeId()         { return feeId; }
    public String    getChallanNumber() { return challanNumber; }
    public String    getStudentName()   { return studentName; }
    public String    getRollNumber()    { return rollNumber; }
    public String    getSemesterName()  { return semesterName; }
    public double    getTotalAmount()   { return totalAmount; }
    public double    getPaidAmount()    { return paidAmount; }
    public LocalDate getGeneratedDate() { return generatedDate; }
    public LocalDate getExpiryDate()    { return expiryDate; }
    public boolean   isPaid()           { return isPaid; }

    public boolean isOverdue() {
        return !isPaid && expiryDate != null
            && expiryDate.isBefore(LocalDate.now());
    }

    /** Display status: Paid / Overdue / Pending */
    public String getDisplayStatus() {
        if (isPaid)     return "Paid";
        if (isOverdue()) return "Overdue";
        return "Pending";
    }
}