package model;

/**
 * Lightweight option for the Major Department dropdown in Add / Edit dialogs.
 */
public class DeptOption {

    private final int    majorDeptId;
    private final String name;
    private final String code;

    public DeptOption(int majorDeptId, String name, String code) {
        this.majorDeptId = majorDeptId;
        this.name        = name;
        this.code        = code;
    }

    public int    getMajorDeptId() { return majorDeptId; }
    public String getName()        { return name; }
    public String getCode()        { return code; }

    @Override public String toString() { return name; }
}
