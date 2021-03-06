package edu.ksu.canvas.attendance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.ksu.canvas.attendance.entity.AttendanceStudent;
import edu.ksu.canvas.attendance.repository.AttendanceStudentRepository;

import java.util.List;


@Component
public class AttendanceStudentService {

    @Autowired
    private AttendanceStudentRepository studentRepository;
    
    
    public AttendanceStudent getStudent(long studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public AttendanceStudent getStudent(String sisId) { return studentRepository.findBySisUserId(sisId) ;}

    public List<AttendanceStudent> getStudentByCourseAndSisId(String sisId, Long canvasCourseId) {
        return studentRepository.findBySisUserIdAndCanvasCourseId(sisId, canvasCourseId) ;
    }

}
