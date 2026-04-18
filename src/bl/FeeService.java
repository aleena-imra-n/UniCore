package bl;

import dao.FeeDAO;
import model.FeeRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Business-Logic layer for Fee Challan and Fee Details features.
 *
 * Zero Swing code. Zero SQL.
 */
public class FeeService {

    public record ChallanResult(Outcome outcome, String message,
                                String challanNumber, String expiryDate) {}

    public enum Outcome { SUCCESS, ALREADY_EXISTS, NO_FEE_RECORD, ERROR }

    private final FeeDAO dao;
    private int     studentId   = -1;
    private int     semesterId  = -1;
    private boolean initialised = false;

    public FeeService()              { this.dao = new FeeDAO(); }
    public FeeService(FeeDAO dao)    { this.dao = dao; }

    public boolean init(String username) {
        try {
            studentId  = dao.getStudentId(username);
            semesterId = dao.getActiveSemesterId();
            initialised = studentId > 0 && semesterId > 0;
            return initialised;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<FeeRecord> getFeeRecords() {
        ensureInit();
        try { return dao.getFeeRecords(studentId); }
        catch (SQLException e) { e.printStackTrace(); return List.of(); }
    }

    public ChallanResult generateChallan() {
        ensureInit();
        try {
            String[] result = dao.generateChallan(studentId, semesterId);
            if (result == null)
                return new ChallanResult(Outcome.ERROR,
                    "Challan generation returned no data.", null, null);
            return new ChallanResult(Outcome.SUCCESS,
                "Challan generated successfully!",
                result[0], result[1]);
        } catch (SQLException e) {
            String msg = e.getMessage();
            // Detect SP RAISERROR messages
            if (msg != null && msg.contains("unpaid challan already exists"))
                return new ChallanResult(Outcome.ALREADY_EXISTS,
                    "An unpaid challan already exists for this semester.", null, null);
            if (msg != null && msg.contains("No fee record"))
                return new ChallanResult(Outcome.NO_FEE_RECORD,
                    "No fee record found for this semester.", null, null);
            return new ChallanResult(Outcome.ERROR,
                "Failed to generate challan: " + msg, null, null);
        }
    }

    public int getStudentId()  { return studentId; }
    public int getSemesterId() { return semesterId; }

    private void ensureInit() {
        if (!initialised)
            throw new IllegalStateException("FeeService.init() must be called first.");
    }
}