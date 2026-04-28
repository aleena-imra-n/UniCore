package bl;

import dao.MyTimetableDAO;
import model.TimetableEntry;

import java.sql.SQLException;
import java.util.*;

/**
 * Business-Logic layer for the My Timetable panel (US-3.7b).
 * Shared by both the teacher and student timetable views.
 *
 * Responsibilities:
 *   - Resolve username → role-specific ID
 *   - Load the flat list of TimetableEntry objects from the DAO
 *   - Group entries by day of week for the weekly grid renderer
 *   - Compute summary stats (total slots, total credit-hour-equivalent minutes)
 *
 * Zero Swing code. Zero SQL.
 */
public class MyTimetableService {

    // ── Canonical day order (Mon–Fri) ─────────────────────────────────────────
    public static final String[] DAYS =
        { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final MyTimetableDAO dao;

    public MyTimetableService()                    { this.dao = new MyTimetableDAO(); }
    public MyTimetableService(MyTimetableDAO dao)  { this.dao = dao; }

    // ── Teacher API ───────────────────────────────────────────────────────────

    /**
     * Loads all timetable entries for the given teacher username.
     *
     * @param username  login username (teacher role)
     * @return list ordered Monday→Friday, then by start_time; empty on error
     */
    public List<TimetableEntry> loadForTeacher(String username) {
        try {
            int id = dao.getTeacherIdByUsername(username);
            if (id < 0) return List.of();
            return dao.getTimetableForTeacher(id);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Student API ───────────────────────────────────────────────────────────

    /**
     * Loads all timetable entries for the given student username.
     *
     * @param username  login username (student role)
     * @return list ordered Monday→Friday, then by start_time; empty on error
     */
    public List<TimetableEntry> loadForStudent(String username) {
        try {
            int id = dao.getStudentIdByUsername(username);
            if (id < 0) return List.of();
            return dao.getTimetableForStudent(id);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // ── Shared grouping ───────────────────────────────────────────────────────

    /**
     * Groups a flat entry list into an ordered map: day → entries for that day.
     * All five weekdays are always present as keys (value may be empty list),
     * so the UI can always render all five columns even if some are empty.
     *
     * @param entries  result of loadForTeacher / loadForStudent
     * @return LinkedHashMap with keys in Mon→Fri order
     */
    public Map<String, List<TimetableEntry>> groupByDay(List<TimetableEntry> entries) {
        Map<String, List<TimetableEntry>> map = new LinkedHashMap<>();
        for (String day : DAYS) map.put(day, new ArrayList<>());
        for (TimetableEntry e : entries) {
            map.computeIfAbsent(e.getDayOfWeek(), k -> new ArrayList<>()).add(e);
        }
        return map;
    }

    /**
     * Counts total weekly class slots.
     */
    public int totalSlots(List<TimetableEntry> entries) {
        return entries.size();
    }

    /**
     * Counts distinct courses (by offering_id) across the whole week.
     */
    public int totalCourses(List<TimetableEntry> entries) {
        Set<Integer> ids = new HashSet<>();
        for (TimetableEntry e : entries) ids.add(e.getOfferingId());
        return ids.size();
    }

    /**
     * Counts total scheduled minutes across all slots this week.
     * Returns 0 if time strings cannot be parsed.
     */
    public int totalMinutes(List<TimetableEntry> entries) {
        int total = 0;
        for (TimetableEntry e : entries) {
            try {
                int sh = Integer.parseInt(e.getStartTime().substring(0, 2));
                int sm = Integer.parseInt(e.getStartTime().substring(3));
                int eh = Integer.parseInt(e.getEndTime().substring(0, 2));
                int em = Integer.parseInt(e.getEndTime().substring(3));
                total += (eh * 60 + em) - (sh * 60 + sm);
            } catch (Exception ignored) {}
        }
        return total;
    }

    /**
     * Formats total minutes as "Xh Ym", e.g. "7h 30m".
     */
    public String formatTotalTime(int minutes) {
        if (minutes <= 0) return "0h";
        return (minutes / 60) + "h" + (minutes % 60 == 0 ? "" : " " + (minutes % 60) + "m");
    }
}