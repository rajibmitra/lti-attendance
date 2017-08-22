<?xml version="1.0" encoding="UTF-8" ?>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- Set context path -->
    <c:set var="context" value="${pageContext.request.contextPath}"/>



    <!-- LOAD BOOTSTRAP -->
    <link rel="stylesheet" href="${context}/bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="${context}/bootstrap/css/bootstrap-theme.css"/>
    <link rel="stylesheet" href="${context}/bootstrap/css/bootstrap-datepicker.min.css"/>
    <link rel="stylesheet" href="${context}/stylesheets/jquery-ui.min.css"/>
    <link rel="stylesheet" href="${context}/stylesheets/style.css"/>
    <link rel="stylesheet" href="${context}/css/buttonOverrides.css"/>

    <%--This needs to be here..--%>
    <script src="${context}/js/jquery.2.1.3.min.js"></script>
    <script src="${context}/js/jquery-ui.min.js"></script>
    <script src="${context}/bootstrap/js/bootstrap.min.js"></script>
    <script src="${context}/js/jquery.confirm.js"></script>
    <script src="${context}/js/scripts.js"></script>

    <title>Class Setup</title>
</head>
<body>
<nav class="navbar navbar-default">
    <div class="container-fluid">
        <div class="navbar-header">
            <a class="navbar-brand" href="${context}/roster/${selectedSectionId}">K-State Attendance</a>
        </div>
        <ul class="nav navbar-nav">
            <li class="active"><a id="classSetupLink"
                                  href="${context}/courseConfiguration/${selectedSectionId}">Setup</a></li>
            <li><a id="attendanceSummaryLink" href="${context}/attendanceSummary/${selectedSectionId}">Attendance
                Summary</a></li>
            <li><a id="rosterLink" href="${context}/roster/${selectedSectionId}">Class Roster</a></li>
            <li><a id="helpLink" href="${context}/help/${selectedSectionId}">Help</a></li>
        </ul>
    </div>
</nav>
<form:form id="setupForm" modelAttribute="courseConfigurationForm" class="sectionDropdown" method="POST"
           action="${context}/courseConfiguration/${selectedSectionId}/save">
    <c:forEach items="${error}" var="oneError">
        <div class="alert alert-danger">
            <p>${oneError}</p>
        </div>
    </c:forEach>
    <c:if test="${pushingSuccessful}">
        <div class="alert alert-success" id="pushingSuccessful">
            <p>Pushing attendance grades to Canvas successful.</p>
        </div>
    </c:if>
<!--There needs to be a message that returns a list of sections that did not successfully push grades to Canvas. It should be grouped with the following success messages. -->
    <c:if test="${updateSuccessful}">
        <div class="alert alert-success">
            <p>Course Setup successfully updated.</p>
        </div>
    </c:if>

    <c:if test="${synchronizationSuccessful}">
        <div class="alert alert-success" id="synchronizationSuccessful">
            <p>Synchronization with canvas was successful.</p>
        </div>
    </c:if>

    <c:if test="${deleteSuccessful}">
        <div class="alert alert-success" id="deleteSuccessful">
            <p>Assignment has been deleted from canvas.</p>
        </div>
    </c:if>


    <h3>Synchronization</h3>

    <p>
        For performance reasons, this application does not automatically synchronize with Canvas. If you notice missing
        students, sections,
        or other problems, please click the button below to rectify the problem. It may take several seconds for this
        operation to complete.
    </p>

    <input value="Synchronize with Canvas" id="synchronizeWithCanvas" name="synchronizeWithCanvas"
           class="hovering-purple-button" type="submit"/>
    <br/>

    <h3>Setup</h3>
    <br/>
    <p> Check out our new <strong><a id="helpLink2" href="${context}/help/${selectedSectionId}">help section</a></strong> for a
        quick guide on how to get the most out of Attendance.</p>
    <div class="col-lg-3">
        <label for="simpleAttendance">
            <form:radiobutton path="simpleAttendance" id="simpleAttendance" value="true"/> Use Simple Attendance (non-minute based)
        </label>
        <br/>
        <label for="aviationAttendance">
            <form:radiobutton path="simpleAttendance" id="aviationAttendance" value="false"/> Use Minute-Based Attendance Accounting (Aviation Maintenance Management)
        </label>
    </div>

    <div class="container-fluid">
        <div id="aviationTimeConfig" class="row ${courseConfigurationForm.simpleAttendance? 'hidden' : '' }">
            <fieldset class="form-inline">
                <div class="col-md-3 col-md-offset-1">
                    <label for="courseWorth">Total Class Minutes</label>
                    <form:input path="totalClassMinutes" type="text" id="courseWorth" cssClass="form-control"
                                placeholder="Total Class Minutes" size="6"/>
                    <form:errors cssClass="error center-block" path="totalClassMinutes"/>
                </div>
                <div class="col-md-3">
                    <label for="defaultMinutesPerSession">Normal Class Length</label>
                    <form:input path="defaultMinutesPerSession" type="text" id="defaultMinutesPerSession"
                                cssClass="form-control" placeholder="Normal Class Length" size="5"/>
                    <form:errors cssClass="error center-block" path="defaultMinutesPerSession"/>
                </div>
            </fieldset>
        </div>
        <label for="showNotesToStudents">
            <form:checkbox path="showNotesToStudents" id="showNotesToStudents"/> Show Notes entered on Class Roster page to students
        </label>


        <div class = "container-fluid" id="conversionConfig" >
            <br/>
            <div class="col-md-2 col-md-offset-0">
                <label for="assignmentName">
                    <h5><i>Assignment Name: </i></h5>
                    <form:input type = "text" path ="assignmentName" id = "assignmentName" size = "15"/>
                </label>
                <br/>
                <label for="assignmentPoints">
                    <h5><i>Total Points: </i></h5>
                    <form:input type = "text" path ="assignmentPoints" id = "assignmentPoints" size = "5"/>
                </label>
                <br/>
            </div>
            <div class="col-md-7 col-md-offset-0">
                <h5><i>Attendance Weights: </i></h5>
                <p>Present, Tardy, Absent, and Excused are possible options for attendance status.
                   Please enter the <strong>percentage</strong> of the total points that each type of status should receive.</p>

                <div class="col-md-2 col-md-offset-0">
                    <label>Present: </label>
                    <br/>
                    <label for="presentPoints">
                        <form:input type = "text" path ="presentPoints" id = "presentPoints" size="7"/>
                    </label>
                </div>
                <div class="col-md-2 col-md-offset-0">
                    <label>Tardy: </label>
                    <br/>
                    <label for="tardyPoints">
                        <form:input type = "text" path ="tardyPoints" id = "tardyPoints" size="7"/>
                    </label>
                </div>
                <div class="col-md-2 col-md-offset-0">
                    <label>Absent: </label>
                    <br/>
                    <label for="absentPoints">
                        <form:input type = "text" path ="absentPoints" id = "absentPoints" size="7"/>
                    </label>
                </div>
                <div class="col-md-2 col-md-offset-0">
                    <label>Excused: </label>
                    <br/>
                    <label for="excusedPoints">
                        <form:input type = "text" path ="excusedPoints" id = "excusedPoints" size="7"/>
                    </label>
                </div>

            </div>

        </div>


        <button id="saveCourseConfiguration" name="saveCourseConfiguration" class="hovering-purple-button pull-left
                buffer-top" type="button" onclick="$('#sectionSelectForSave').modal('show')">
            Save Setup
        </button>

        <button id="setupSummary" name="setupSummary" class="hovering-purple-button buffer-top buffer-left " type="button" onclick="$('#savedSetupView').modal('show')">
            View Saved Setups
        </button>

        <button id="deleteAssignments" name="deleteAssignments" class="hovering-purple-button buffer-top buffer-left " type="button" onclick="$('#deleteModal').modal('show')">
            Delete Assignments
        </button>

        <button id="pushConfirmation" name="pushConfirmation" class=" ${error == null? 'hovering-purple-button':'button_disabled'}
                pull-right buffer-top" type="button"
                ${error == null? '':'disabled'} onclick="$('#sectionSelect').modal('show')">
            Push Attendance to Gradebook
        </button>
    </div>
    <hr/>

    <div class="confirmation-modal modal fade in" id = "deleteModal">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Deletion Confirmation</h4>
                </div>
                <div class="modal-body">
                    <div class="alert alert-danger"><p>Select which Assignments to delete from Canvas:</p>
                    </div>
                    <c:forEach items="${courseConfigurationForm.allSections}" var="section">
                        <br>
                        <form:checkbox disabled="${section.attendanceAssignment.assignmentName != null ? '': 'true'}"
                                       path="sectionsToDelete" id="${section.canvasSectionId}" value="${section.canvasSectionId}"
                                       name="deleteList"/> ${section.name}
                    </c:forEach>
                </div>
                <div class="modal-footer">
                    <button id="deleteAssignment" name="deleteAssignment" class="confirm btn btn-primary" type="button" onclick="submitDeleteForm()">
                        Yes
                    </button>
                    <button class="confirm btn btn-default" type="button" onclick="$('#deleteModal').modal('hide')">
                        No
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="confirmation-modal modal fade in" id = "sectionSelect">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Select which Sections will have assignments:</h4>
                </div>
                <div class="modal-body">
                    <div class="alert alert-warning"><p>Please allow a few minutes for Canvas to update the gradebook.</p>
                    </div>
                        <c:forEach items="${courseConfigurationForm.allSections}" var="section">
                            <br>
                                <form:checkbox disabled="${section.attendanceAssignment.assignmentName != null ? '': 'true'}"
                                               path="sectionsToGrade" id="${section.canvasSectionId}" value="${section.canvasSectionId}"
                                               name="pushList"/> ${section.name}
                        </c:forEach>
                </div>
                <div class="modal-footer">
                    <button id="sectionSubmit" name="sectionSubmit" class="confirm btn btn-primary" type="button" onclick="submitPushForm();">
                        OK
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="confirmation-modal modal fade in" id = "savedSetupView">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Saved Setups</h4>
                </div>
                <div class="modal-body">
                    <table class="table table-bordered">

                        <thead>
                        <tr>
                            <th class="col-md-3">Assignment Name</th>
                            <th class="col-md-3">Total Points</th>
                            <th class="col-md-4">Present Percentage</th>
                            <th class="col-md-4">Tardy Percentage</th>
                            <th class="col-md-4">Absent Percentage</th>
                            <th class="col-md-4">Excused Percentage</th>
                            <th class="col-md-4">Associated Section</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${courseConfigurationForm.allSections}" var="section">
                            <c:if test="${section.attendanceAssignment.assignmentName != null}">
                                <tr>
                                    <td class="col-md-3">${section.attendanceAssignment.assignmentName}</td>
                                    <td class="col-md-3">${section.attendanceAssignment.assignmentPoints}</td>
                                    <td class="col-md-3">${section.attendanceAssignment.presentPoints}</td>
                                    <td class="col-md-3">${section.attendanceAssignment.tardyPoints}</td>
                                    <td class="col-md-3">${section.attendanceAssignment.absentPoints}</td>
                                    <td class="col-md-3">${section.attendanceAssignment.excusedPoints}</td>
                                    <td class="col-md-3">${section.name}</td>
                                </tr>
                            </c:if>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="modal-footer">
                    <button id="closeSavedSetupView" name="closeSavedSetupView" class="close btn btn-primary" type="button" onclick="$('#savedSetupView').modal('hide');">
                        Close
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="confirmation-modal modal fade in" id = "sectionSelectForSave">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Select which Sections this setup needs to be saved for:</h4>
                </div>
                <div class="modal-body">
                    <input type="checkbox" id="selectSaved" name="selectSaved" onclick="toggle(this, 'savedList')" />
                    <label for="selectSaved">Select All</label>
                    <br>
                    <c:forEach items="${courseConfigurationForm.allSections}" var="section">
                        <br>
                        <form:checkbox path="sectionsToSave" id="${section.canvasSectionId}" value="${section.canvasSectionId}"
                                       name="savedList"/> ${section.name}
                    </c:forEach>
                </div>
                <div class="modal-footer">
                    <button id="sectionSave" name="sectionSave" class="confirm btn btn-primary" type="button" onclick="submitSaveForm();">
                        OK
                    </button>
                </div>
            </div>
        </div>
    </div>

    <br/><br/>
    <script type="text/javascript">

        var errorMessage = "There was an error communicating with the server.";

        $('#simpleAttendance').change(function(){
            if (this.checked) {
                $('#aviationTimeConfig').addClass('hidden');
            }
        });
        $('#aviationAttendance').change(function(){
            if (this.checked) {
                $('#aviationTimeConfig').removeClass('hidden');
            }
        });


        $("#assignmentName, #assignmentPoints, #presentPoints, #tardyPoints, #absentPoints, #excusedPoints").keyup(function () {
            disablePushConfirmation();
        });

        function submitPushForm(){
            $("<input />")
                    .attr("type", "hidden")
                    .attr("name", "pushGradesToCanvas")
                    .attr("value", "pushGradesToCanvas")
                    .appendTo("#setupForm");
            $('#setupForm').submit();
            $('#sectionSubmit').attr("disabled", "disabled");
            $("input:checkbox").each(function(){
                this.disabled = true;
            });

        }
        function submitSaveForm(){
            $("<input />")
                    .attr("type", "hidden")
                    .attr("name", "saveCourseConfiguration")
                    .attr("value", "saveCourseConfiguration")
                    .appendTo("#setupForm");
            $('#setupForm').submit();
            $('#saveCourseConfiguration').attr("disabled", "disabled");
        }
        function submitDeleteForm(){
            $("<input />")
                    .attr("type", "hidden")
                    .attr("name", "deleteAssignment")
                    .attr("value", "deleteAssignment")
                    .appendTo("#setupForm");
            $('#setupForm').submit();
            $('#deleteAssignment').attr("disabled", "disabled");
            $('#cancelDelete').attr("disabled", "disabled");
        }

        function toggle(source, name){
            console.log(source);
            console.log(name);
            console.log('------------------------------------------------------');
            var checkboxes = document.getElementsByName("savedList");
            console.log(checkboxes.length);
            for(var i=0; i < checkboxes.length; i++){
                console.log("ayy");
                checkboxes[i].checked = source.checked;
            }
        }



    </script>

</form:form>

</body>
</html>
