package edu.ksu.canvas.aviation.services;

import edu.ksu.canvas.aviation.entity.Attendance;
import edu.ksu.canvas.aviation.entity.AviationCourse;
import edu.ksu.canvas.aviation.entity.AviationStudent;
import edu.ksu.canvas.aviation.entity.MakeupTracker;
import edu.ksu.canvas.aviation.enums.Status;
import edu.ksu.canvas.aviation.form.MakeupTrackerForm;
import edu.ksu.canvas.aviation.form.RosterForm;
import edu.ksu.canvas.aviation.model.SectionInfo;
import edu.ksu.canvas.aviation.repository.AttendanceRepository;
import edu.ksu.canvas.aviation.repository.AviationCourseRepository;
import edu.ksu.canvas.aviation.repository.AviationStudentRepository;
import edu.ksu.canvas.aviation.repository.MakeupTrackerRepository;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author jesusorr
 * 5/6/16
 */
@Component
public class PersistenceService {
    private static final Logger LOG = Logger.getLogger(PersistenceService.class);
    private static final int DEFAULT_TOTAL_CLASS_MINUTES = 2160; //DEFAULT_MINUTES_PER_CLASS * 3 days a week * 16 weeks
    private static final int DEFAULT_MINUTES_PER_CLASS = 45;

    @Autowired
    private AviationCourseRepository aviationCourseRepository;

    @Autowired
    private AviationStudentRepository aviationStudentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private MakeupTrackerRepository makeupTrackerRepository;
    

    public void saveCourseMinutes(RosterForm rosterForm, String courseId) {

        Long canvasCourseId = Long.parseLong(courseId);
        AviationCourse aviationCourse = aviationCourseRepository.findByCanvasCourseId(canvasCourseId);
        if(aviationCourse == null){
            aviationCourse = new AviationCourse(canvasCourseId, rosterForm.getTotalClassMinutes(), rosterForm.getDefaultMinutesPerSession());
        }
        else{
            aviationCourse.setDefaultMinutesPerSession(rosterForm.getDefaultMinutesPerSession());
            aviationCourse.setTotalMinutes(rosterForm.getTotalClassMinutes());
        }

        aviationCourseRepository.save(aviationCourse);
    }
    
    public void saveMakeups(MakeupTrackerForm form) {
        
        for(MakeupTracker makeup: form.getEntries()) {
            if(makeup.getMakeupTrackerId() == null) {
                AviationStudent student = aviationStudentRepository.findByStudentId(form.getStudentId());
                makeup.setAviationStudent(student);
                makeupTrackerRepository.save(makeup);
            } else {
                MakeupTracker tracker = makeupTrackerRepository.findByMakeupTrackerId(makeup.getMakeupTrackerId());
                tracker.setDateMadeUp(makeup.getDateMadeUp());
                tracker.setDateOfClass(makeup.getDateOfClass());
                tracker.setMinutesMadeUp(makeup.getMinutesMadeUp());
                makeupTrackerRepository.save(tracker);
            }

        }
    }
    
    public void deleteMakeup(String makeupTrackerId) {
        makeupTrackerRepository.delete(Long.valueOf(makeupTrackerId));
    }

    public void saveClassAttendance(RosterForm rosterForm) {
        for (SectionInfo sectionInfo : rosterForm.getSectionInfoList()){
            LOG.info("Saving section: " + sectionInfo);
            List<Attendance> attendancesToSave = new ArrayList<>();
            for(AviationStudent aviationStudent : sectionInfo.getStudents()) {
                //Save students
                AviationStudent persistedStudent = aviationStudentRepository.findBySisUserIdAndSectionId(aviationStudent.getSisUserId(), sectionInfo.getSectionId());
                if(persistedStudent == null) {
                    LOG.debug("Saving student: " + aviationStudent);
                    persistedStudent = aviationStudentRepository.save(aviationStudent);
                } else {
                    //Update the attendances
                    persistedStudent.setAttendances(aviationStudent.getAttendances());
                }
                for (Attendance attendance : persistedStudent.getAttendances()) {
                    attendance.setAviationStudent(persistedStudent);
                    if (DateUtils.isSameDay(attendance.getDateOfClass(), rosterForm.getCurrentDate())){
                        attendancesToSave.add(attendance);
                    }
                }
            }
            attendanceRepository.save(attendancesToSave);
        }
    }
    
    public RosterForm loadOrCreateCourseMinutes(RosterForm rosterForm, String courseId) {
        Long canvasCourseId = Long.parseLong(courseId);
        AviationCourse aviationCourse = aviationCourseRepository.findByCanvasCourseId(canvasCourseId);
        if (aviationCourse != null && aviationCourse.getTotalMinutes() != null && aviationCourse.getDefaultMinutesPerSession() != null){
            rosterForm.setTotalClassMinutes(aviationCourse.getTotalMinutes());
            rosterForm.setDefaultMinutesPerSession(aviationCourse.getDefaultMinutesPerSession());
        } else {
            rosterForm.setTotalClassMinutes(DEFAULT_TOTAL_CLASS_MINUTES);
            rosterForm.setDefaultMinutesPerSession(DEFAULT_MINUTES_PER_CLASS);
        }
        return rosterForm;
    }

    /**
     * DO NOT USE ON POST BACK You will get a lazy load exception
     * @param date some date we want to populate
     * @return the same roster form you gave me but populated with attendance for this date
     */
    public RosterForm populateAttendanceForDay(RosterForm rosterForm, Date date) {
        rosterForm.getSectionInfoList().forEach(section -> {
            section.getStudents().forEach(student -> {
                if (student.getAttendances() == null) {
                    LOG.debug("Initializing new attendance list for student: " + student);
                    student.setAttendances(new ArrayList<>());
                }
                if (student.getAttendances().stream()
                        .noneMatch(attendance -> DateUtils.isSameDay(attendance.getDateOfClass(), date))) {
                    //create a new default attendance since we don't have one for this day
                    LOG.debug("Creating default attendance for student "+ student);
                    Attendance attendance = new Attendance(student,Status.PRESENT, date);
                    student.getAttendances().add(attendance);
                }
            });
        });
        return rosterForm;
    }
}
