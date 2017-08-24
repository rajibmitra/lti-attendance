package edu.ksu.canvas.attendance.submitter;


import edu.ksu.canvas.attendance.entity.Attendance;
import edu.ksu.canvas.attendance.entity.AttendanceAssignment;
import edu.ksu.canvas.attendance.entity.AttendanceCourse;
import edu.ksu.canvas.attendance.entity.AttendanceSection;
import edu.ksu.canvas.attendance.exception.AttendanceAssignmentException;
import edu.ksu.canvas.attendance.model.AttendanceSummaryModel;
import edu.ksu.canvas.attendance.services.*;
import edu.ksu.canvas.model.Progress;
import edu.ksu.canvas.model.assignment.Assignment;
import edu.ksu.canvas.model.assignment.AssignmentGroup;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.MultipleSubmissionsOptions;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Scope(value="session")
public class AssignmentSubmitter {
    private static final Logger LOG = Logger.getLogger(AssignmentSubmitter.class);

    @Autowired
    private AttendanceAssignmentService assignmentService;

    @Autowired
    private AttendanceSectionService sectionService;

    @Autowired
    private AttendanceCourseService attendanceCourseService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CanvasApiWrapperService canvasApiWrapperService;

    @Autowired
    private AssignmentValidator assignmentValidator;

    @Autowired
    private CanvasAssignmentAssistant canvasAssignmentAssistant;


    /**
     * This function takes the summary for sections and one by one validates the attendance assignment and
     * the canvas assignment. Then pushes all the grades and comments to the canvas assignment associated.
     * @param isSimpleAttendance
     * @param summaryForSections
     */
    public void submitCourseAttendances(boolean isSimpleAttendance, List<AttendanceSummaryModel> summaryForSections, Long courseId,
                                        OauthToken oauthToken, List<AttendanceSection> sectionsToGrade) throws AttendanceAssignmentException, IOException{

        for (AttendanceSection section: sectionsToGrade) {
            for (AttendanceSummaryModel sectionSummary : summaryForSections){
                if (sectionSummary.getSectionId() == section.getCanvasSectionId()){
                    AttendanceAssignment attendanceAssignment = section.getAttendanceAssignment();

                    gradePushingValidation(courseId, section.getCanvasSectionId(), oauthToken, sectionSummary, attendanceAssignment);

                    submitSectionAttendances(isSimpleAttendance, sectionSummary, attendanceAssignment, courseId, oauthToken);
                }
            }
        }
    }

    /**
     * If in validation is determined discrepancy in the data between canvas and teh assignment in the data base,
     * canvas will be aligned to the data in the db. If in validation is determined that there is not canvas assignment
     * associated to the attendance assignment then a new canvas assignment will be created and associate to the attendance assignment.
     */
    private void gradePushingValidation(Long courseId, Long canvasSectionId, OauthToken oauthToken, AttendanceSummaryModel model,
                                        AttendanceAssignment attendanceAssignment) throws AttendanceAssignmentException, IOException{

        if (attendanceAssignment.getStatus() == AttendanceAssignment.Status.UNKNOWN){
            attendanceAssignment = assignmentValidator.validateAttendanceAssignment(courseId, attendanceAssignment, canvasApiWrapperService, oauthToken);
        }
        if (attendanceAssignment.getStatus() == AttendanceAssignment.Status.UNKNOWN){
            attendanceAssignment = assignmentValidator.validateCanvasAssignment(courseId, attendanceAssignment, canvasApiWrapperService, oauthToken);
        }
        if (attendanceAssignment.getStatus() == AttendanceAssignment.Status.CANVAS_AND_DB_DISCREPANCY){
            canvasAssignmentAssistant.editAssignmentInCanvas(courseId, attendanceAssignment, oauthToken);
        }
        else if (attendanceAssignment.getStatus() == AttendanceAssignment.Status.NOT_LINKED_TO_CANVAS){

            List<AssignmentGroup> assignmentGroups = canvasApiWrapperService.listAssignmentGroups(courseId.toString(),oauthToken);
            Long defaultGroupId = 0L;

            for (AssignmentGroup group: assignmentGroups){
                if (group.getName().equals("Attendance")){
                    defaultGroupId = group.getId();
                    break;
                }
            }
            if (defaultGroupId == 0L){
                AssignmentGroup group = canvasApiWrapperService.createAssignmentGroup(courseId.toString(), oauthToken);
                defaultGroupId = group.getId();
            }
            Assignment assignment = canvasAssignmentAssistant.createAssignmentInCanvas(courseId, canvasSectionId, attendanceAssignment, defaultGroupId, oauthToken);

            canvasApiWrapperService.createAssignmentOverride(oauthToken, Integer.valueOf(assignment.getId()), (int)model.getSectionId(), courseId.toString());
            canvasApiWrapperService.setAssignmentOnlyVisibleToOverrides(oauthToken, courseId.toString(), assignment);
        }
        attendanceAssignment.setStatus(AttendanceAssignment.Status.OKAY);

    }

    /**
     * Handles the push grades and/or comments to canvas for just one section.
     */
    private void submitSectionAttendances(boolean isSimpleAttendance, AttendanceSummaryModel model, AttendanceAssignment attendanceAssignment, Long courseId, OauthToken oauthToken) throws AttendanceAssignmentException {
        Map<String, MultipleSubmissionsOptions.StudentSubmissionOption> studentMap;
        MultipleSubmissionsOptions submissionOptions;
        studentMap = new HashMap<>();
        submissionOptions = new MultipleSubmissionsOptions(((Long) model.getSectionId()).toString(), attendanceAssignment.getCanvasAssignmentId().intValue(), null);

        AttendanceCourse course = attendanceCourseService.findByCanvasCourseId(courseId);

        Map<Long, String> studentCommentsMap = new HashMap<>();
        if (course.getShowNotesToStudents()) {
            studentCommentsMap = attendanceService.getAttendanceCommentsBySectionId(model.getSectionId());
        }

        //Generates the map with the information to be submitted to Canvas
        for (AttendanceSummaryModel.Entry entry : model.getEntries()) {
            if (!entry.isDropped() && entry.getSisUserId() != null) {
                studentMap.put("sis_user_id:" + entry.getSisUserId(),
                        generateStudentSubmissionOptions(isSimpleAttendance, submissionOptions, attendanceAssignment, course, studentCommentsMap, entry));
            }
        }

        //Pushing the information to Canvas
        Optional<Progress> returnedProgress;
        submissionOptions.setStudentSubmissionOptionMap(studentMap);
        try {
            returnedProgress = canvasApiWrapperService.gradeMultipleSubmissionsBySection(oauthToken, submissionOptions);
        } catch (IOException e) {
            LOG.error("Error while pushing the grades of section: " + model.getSectionId(), e);
            throw new AttendanceAssignmentException(AttendanceAssignmentException.Error.FAILED_PUSH);
        }

        if (!isValidProgress(returnedProgress)) {
            LOG.error("Error object returned while pushing the grades of section: " + model.getSectionId());
            throw new AttendanceAssignmentException(AttendanceAssignmentException.Error.FAILED_PUSH);
        }
    }

    /**
     * Generates the parameters and data needed for push grades or comments to canvas
     */
    private MultipleSubmissionsOptions.StudentSubmissionOption generateStudentSubmissionOptions(boolean isSimpleAttendance, MultipleSubmissionsOptions submissionOptions,
                                                                                               AttendanceAssignment attendanceAssignment, AttendanceCourse course, Map<Long, String> studentCommentsMap, AttendanceSummaryModel.Entry entry) {

        Double grade = calculateStudentGrade(attendanceAssignment, entry, isSimpleAttendance);
        StringBuilder comments = new StringBuilder(getCommentHeader(entry, course, isSimpleAttendance));

        if (course.getShowNotesToStudents() && studentCommentsMap.keySet().contains(entry.getStudentId())) {
            comments.append("\nAdditional Comments: \n");
            comments.append(studentCommentsMap.get(entry.getStudentId()));
        }

        return submissionOptions.createStudentSubmissionOption(comments.toString(), grade.toString(), null, null, null, null);
    }

    private boolean isValidProgress(Optional<Progress> returnedProgress) {
        return returnedProgress.isPresent() && !"failed".equals(returnedProgress.get().getWorkflowState());
    }

    /**
     * Generates the header part of the comments, which is an explanation of the grades.
     */
    private String getCommentHeader(AttendanceSummaryModel.Entry entry, AttendanceCourse course, boolean isSimpleAttendance) {
        if (isSimpleAttendance) {
            return "Total number of classes: " + getTotalSimpleClasses(entry) + "\nNumber of classes present: " + entry.getTotalClassesPresent()
                    + "\nNumber of classes tardy: " + entry.getTotalClassesTardy() + "\nNumber of classes absent: " + entry.getTotalClassesMissed()
                    + "\nNumber of classes excused: " + entry.getTotalClassesExcused() + "\n";
        } else {
            return "Total number of minutes: " + course.getTotalMinutes() + "\nTotal minutes missed: " + entry.getSumMinutesMissed()
                    + "\nNumber of minutes made up: " + entry.getSumMinutesMadeup() + "\nNumber of minutes remaining to be made up: "
                    + entry.getRemainingMinutesMadeup() + "\n";
        }
    }

    /**
     * Calculates the grade of one student.
     */
    private double calculateStudentGrade(AttendanceAssignment attendanceAssignment, AttendanceSummaryModel.Entry entry, boolean isSimpleAttendance) {
        String presentPoints = attendanceAssignment.getPresentPoints();
        String tardyPoints = attendanceAssignment.getTardyPoints();
        String excusedPoints = attendanceAssignment.getExcusedPoints();
        String absentPoints = attendanceAssignment.getAbsentPoints();

        if (isSimpleAttendance) {
            int totalClasses = getTotalSimpleClasses(entry);
            totalClasses = totalClasses == 0? 1 : totalClasses;
            return ((((entry.getTotalClassesPresent() * (calculation(presentPoints))) +
                    (entry.getTotalClassesTardy() * (calculation(tardyPoints))) +
                    (entry.getTotalClassesExcused() * (calculation(excusedPoints))) +
                    (entry.getTotalClassesMissed() * (calculation(absentPoints)))) / totalClasses) * Double.parseDouble(attendanceAssignment.getAssignmentPoints()));
        } else {

            return ((100 - entry.getPercentCourseMissed()) / 100) * Double.parseDouble(attendanceAssignment.getAssignmentPoints());
        }
    }
    private double calculation(String value) {
        return Double.parseDouble(value)/100;
    }

    private int getTotalSimpleClasses(AttendanceSummaryModel.Entry entry) {
        return entry.getTotalClassesExcused() + entry.getTotalClassesMissed() + entry.getTotalClassesTardy() + entry.getTotalClassesPresent();
    }

    private boolean checkForAttendanceGroup(List<AssignmentGroup> assignmentGroups){
        boolean groupExists = false;
        for(AssignmentGroup group: assignmentGroups){
            if (group.getName().equals("Attendance")){
                groupExists = true;
            }
        }
        return groupExists;
    }

}