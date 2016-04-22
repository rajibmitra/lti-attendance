package edu.ksu.canvas.aviation.controller;

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
import edu.ksu.canvas.impl.EnrollmentsImpl;
import edu.ksu.canvas.impl.SectionsImpl;
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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@Scope("request")
public class AviationReportingController extends LtiLaunchController {
    private static final Logger LOG = Logger.getLogger(AviationReportingController.class);
    private static final int CANVAS_VERSION = 1;

    @Autowired
    protected LtiLaunch ltiLaunch;

    @Autowired
    private RoleChecker roleChecker;
//
//    @Autowired
//    private SectionReader sectionReader;


    @Autowired
    private ConfigRepository configRepository;
//
//    @Autowired
//    private RestClient restClient;

    @RequestMapping("/")
    public ModelAndView home(HttpServletRequest request) {
        LOG.info("Showing Activity Reporting configuration XML");
        String ltiLaunchUrl = AppConfig.getApplicationBaseUrl(request, true) + "/launch";
        LOG.debug("LTI launch URL: " + ltiLaunchUrl);
        return new ModelAndView("ltiConfigure", "url", ltiLaunchUrl);
    }

    @RequestMapping("/showRoster")
    public ModelAndView showRoster(ModelMap modelMap, @ModelAttribute RosterForm rosterForm) throws NoLtiSessionException, OauthTokenRequiredException, InvalidInstanceException, IOException {

        ltiLaunch.ensureApiTokenPresent(getApplicationName());
        ltiLaunch.validateOAuthToken();
        LtiSession ltiSession = ltiLaunch.getLtiSession();
        assertPrivilegedUser(ltiSession);
        OauthToken oauthToken = ltiSession.getCanvasOauthToken();
        RestClient restClient = new RestClientImpl();

        ConfigItem configItem = configRepository.findByLtiApplicationAndKey("COMMON", "canvas_url");
        String canvasBaseUrl = configItem.getValue();
        EnrollmentsReader enrollmentsReader = new EnrollmentsImpl(canvasBaseUrl, CANVAS_VERSION, oauthToken.getToken(), restClient);

//        String eid = ltiSession.getEid();
        String courseID = ltiSession.getCanvasCourseId();
        SectionIncludes studentsSection = SectionIncludes.students;

        SectionReader sectionReader = new SectionsImpl(canvasBaseUrl, CANVAS_VERSION, oauthToken.getToken(), restClient);
        List<SectionIncludes> sectionIncludesList = new ArrayList<>();
        sectionIncludesList.add(studentsSection);
        List<Section> sections = sectionReader.listCourseSections(Integer.parseInt(courseID), sectionIncludesList);

        EnrollmentType type = EnrollmentType.STUDENT;
        List<EnrollmentType> enrollmentTypes = new ArrayList<>();
        enrollmentTypes.add(type);

        // Get section data
        // FIXME: For now using JSON, will later save and retrieve data for dates, attendance, from database
        List<SectionInfo> sectionInfoList = new ArrayList<>();
        for(Section s: sections) {
            SectionInfo sectionInfo = new SectionInfo();
            List<Enrollment> enrollments = enrollmentsReader.getSectionEnrollments(oauthToken.getToken(), (int)s.getId(), enrollmentTypes);
            List<Student> students = new ArrayList<>();
            for (Enrollment e: enrollments) {
                Student student = new Student();
                // FIXME: This will be the WID
                student.setId(e.getId());
                student.setName(e.getUser().getSortableName());
                students.add(student);
                LOG.info("Student: " + student.getName());
            }
            sectionInfo.setTotalStudents(students.size());
            if (students.size() > 0) {
                sectionInfo.setStudents(students);
                sectionInfo.setSectionId(s.getId());
                sectionInfo.setSectionName(s.getName());
                sectionInfo.setCourseId(s.getCourseId());
                sectionInfoList.add(sectionInfo);
            }

//            sectionInfo.setTotalStudents(s.getTotalStudents());

            //TODO: Read in fake DAY and ATTENDANCE data from JSON

        }
        ModelAndView page = new ModelAndView("showRoster");
        page.addObject("rosterForm", rosterForm);

        return page;
    }


    @RequestMapping("/save")
    public String save(@ModelAttribute RosterForm rosterForm) {

        return "redirect:/displayRoster";
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