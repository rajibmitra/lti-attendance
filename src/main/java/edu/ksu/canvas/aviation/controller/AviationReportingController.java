package edu.ksu.canvas.aviation.controller;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.aviation.config.AppConfig;
import edu.ksu.canvas.aviation.form.RosterForm;
import edu.ksu.canvas.aviation.model.SectionInfo;
import edu.ksu.canvas.aviation.model.Student;
import edu.ksu.canvas.aviation.util.RoleChecker;
import edu.ksu.canvas.entity.config.ConfigItem;
import edu.ksu.canvas.entity.lti.OauthToken;
import edu.ksu.canvas.enums.EnrollmentType;
import edu.ksu.canvas.enums.SectionIncludes;
import edu.ksu.canvas.error.InvalidInstanceException;
import edu.ksu.canvas.error.NoLtiSessionException;
import edu.ksu.canvas.error.OauthTokenRequiredException;
import edu.ksu.canvas.interfaces.EnrollmentsReader;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.net.RestClientImpl;
import edu.ksu.canvas.repository.ConfigRepository;
import edu.ksu.lti.LtiLaunch;
import edu.ksu.lti.LtiLaunchData;
import edu.ksu.lti.controller.LtiLaunchController;
import edu.ksu.lti.model.LtiSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@Scope("session")
@SessionAttributes("rosterForm")
public class AviationReportingController extends LtiLaunchController {
    private static final Logger LOG = Logger.getLogger(AviationReportingController.class);
    private static final int CANVAS_VERSION = 1;

    @Autowired
    protected LtiLaunch ltiLaunch;

    @Autowired
    private RoleChecker roleChecker;

    @Autowired
    private ConfigRepository configRepository;

    @RequestMapping("/")
    public ModelAndView home(HttpServletRequest request) {
        LOG.info("Showing Activity Reporting configuration XML");
        String ltiLaunchUrl = AppConfig.getApplicationBaseUrl(request, true) + "/launch";
        LOG.debug("LTI launch URL: " + ltiLaunchUrl);
        return new ModelAndView("ltiConfigure", "url", ltiLaunchUrl);
    }

    @RequestMapping("/showRoster")
    public ModelAndView showRoster() throws NoLtiSessionException, OauthTokenRequiredException, InvalidInstanceException, IOException {
        RosterForm rosterForm = new RosterForm();
        ltiLaunch.ensureApiTokenPresent(getApplicationName());
        ltiLaunch.validateOAuthToken();
        LtiSession ltiSession = ltiLaunch.getLtiSession();
        assertPrivilegedUser(ltiSession);
        OauthToken oauthToken = ltiSession.getCanvasOauthToken();

        ConfigItem configItem = configRepository.findByLtiApplicationAndKey("COMMON", "canvas_url");
        String canvasBaseUrl = configItem.getValue();
        CanvasApiFactory canvasApiFactory = new CanvasApiFactory(canvasBaseUrl);
        // FIXME: Timeouts need to change
        EnrollmentsReader enrollmentsReader = canvasApiFactory.getReader(EnrollmentsReader.class, oauthToken.getToken());

        String courseID = ltiSession.getCanvasCourseId();
        SectionIncludes studentsSection = SectionIncludes.students;

        SectionReader sectionReader = canvasApiFactory.getReader(SectionReader.class, oauthToken.getToken());
        List<SectionIncludes> sectionIncludesList = new ArrayList<>();
        sectionIncludesList.add(studentsSection);
        List<Section> sections = sectionReader.listCourseSections(Integer.parseInt(courseID), sectionIncludesList);

        EnrollmentType type = EnrollmentType.STUDENT;
        List<EnrollmentType> enrollmentTypes = new ArrayList<>();
        enrollmentTypes.add(type);

        // Get section data
        // FIXME: Retrieve data for dates, attendance, from a database
        List<SectionInfo> sectionInfoList = new ArrayList<>();
        for(Section s: sections) {
            SectionInfo sectionInfo = new SectionInfo();
            List<Enrollment> enrollments = enrollmentsReader.getSectionEnrollments((int)s.getId(), enrollmentTypes);
            List<Student> students = new ArrayList<>();
            for (Enrollment e: enrollments) {
                Student student = new Student();
                student.setId(Integer.parseInt(e.getUser().getSisUserId()));
                student.setName(e.getUser().getSortableName());
                students.add(student);
            }
            sectionInfo.setTotalStudents(students.size());
            if (students.size() > 0) {
                sectionInfo.setStudents(students);
                sectionInfo.setSectionId(s.getId());
                sectionInfo.setSectionName(s.getName());
                sectionInfo.setCourseId(s.getCourseId());
                sectionInfoList.add(sectionInfo);
            }
            // replace
//            JsonFileParseUtil jsonFileParseUtil = new JsonFileParseUtil();
//            List<Day> days = jsonFileParseUtil.loadDaysFromJson("generated.json");
//            sectionInfo.setDays(days);
        }
        rosterForm.setSectionInfoList(sectionInfoList);
        ModelAndView page = new ModelAndView("showRoster");
        page.addObject("rosterForm", rosterForm);

        return page;
    }

    // TODO: implement
    private RosterForm getRosterForm() { return null; }

    // TODO: Implement Save
    @RequestMapping(value = "/saveAttendance")
    public String saveAttendance(@ModelAttribute("rosterForm") RosterForm rosterForm) throws IOException, NoLtiSessionException {
//        LtiSession ltiSession = ltiLaunch.getLtiSession();
//        // TODO: Parse changes to json file
//        JsonFileParseUtil jsonFileParseUtil = new JsonFileParseUtil();
//        List<Day> days = new ArrayList<Day>();
//        LOG.info("Roster form size: " + rosterForm.getSectionInfoList());
//        for (int i = 0; i < rosterForm.getSectionInfoList().size(); i++) {
//            if (rosterForm.getSectionInfoList().get(i).getSectionName().equals("CIS 200 A")) {
//                days = rosterForm.getSectionInfoList().get(i).getDays();
//            }
//        }
////        for(int i = 0; i < days.size(); i++){
////            for(int j = 0; j < days.get(i).getAttendances().size(); j++){
////                LOG.debug(days.get(i).getAttendances().get(j).getId());
////                LOG.debug(days.get(i).getAttendances().get(j).getMinutesMissed());
////                LOG.debug(days.get(i).getAttendances().get(j).getPercentageMissed());
////                LOG.debug(days.get(i).getAttendances().get(j).getDateOfClass());
////            }
////        }
//        jsonFileParseUtil.writeDaysToJson("saveDates.json", days);
        return "showRoster";
    }

    @RequestMapping(value = "/selectSectionDropdown", method = RequestMethod.POST)
    public ModelAndView selectSection(@ModelAttribute("selectedSection") SectionInfo sectionInfo, @ModelAttribute RosterForm rosterForm){

        //NOTE: this is temporary, set to get date 2016/04/21
        Calendar myCal = Calendar.getInstance();
        myCal.set(Calendar.YEAR, 2016);
        myCal.set(Calendar.MONTH, 4);
        myCal.set(Calendar.DAY_OF_MONTH, 21);
        Date theDate = myCal.getTime();

//        Day day = sectionInfo.getDays().stream()
//                .filter(x -> x.getDate() == theDate)
//                .findFirst()
//                .get();
//
//        Student students = sectionInfo.getStudents().stream()
//                .filter(x -> x.getId() == day.getId())
//                .findFirst()
//                .get();

        LOG.info("THINGS AND STUFF");
        ModelAndView page = new ModelAndView("showRoster");
        page.addObject("selectedSection", sectionInfo);
        page.addObject("rosterForm", rosterForm);
        return page;
    }


     @Override
    protected String getInitialViewPath() {
        return "/showRoster";
    }

    @Override
    protected String getApplicationName() {
        return "AviationReporting";
    }

    private void assertPrivilegedUser(LtiSession ltiSession) throws NoLtiSessionException, AccessDeniedException {
        if (ltiSession.getEid() == null || ltiSession.getEid().isEmpty()) {
            throw new AccessDeniedException("You cannot access this content without a valid session");
        }
        LtiLaunchData launchData = ltiSession.getLtiLaunchData();
        List<LtiLaunchData.InstitutionRole> roles = launchData.getRolesList();
        if (!roleChecker.roleAllowed(roles)) {
            LOG.error("User (" + ltiSession.getEid() + ") with insufficient privileges tried to launch. Roles: " + ltiSession.getLtiLaunchData().getRoles());
            throw new AccessDeniedException("You do not have sufficient privileges to use this tool");
        }
    }
}