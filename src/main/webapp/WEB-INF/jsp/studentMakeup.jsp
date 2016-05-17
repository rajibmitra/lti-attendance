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
  <c:set var="context" value="${pageContext.request.contextPath}" />


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
  <script src="${context}/js/scripts.js"></script>

    <script type="text/javascript">
        function generateRow(currentRow) {
            return '<tr id="row-' + currentRow +'">' +
                    '<td>' +
                    '<input type="hidden" id="id' + currentRow + '" path="entries[' + currentRow + '].makeupId" />' +
                    '<div class="form-group">' +
                    '<div class="input-group date" id="datePickerDateOfClass-' + currentRow + '">' +
                    '<input required="true" id="classDate' + currentRow + '" path="entries[' + currentRow + '].dateOfClass" cssClass="form-control" />' +
                    '<span class="input-group-addon" style="display: inline;"> <span class="glyphicon glyphicon-calendar"></span>' +
                    '</span>' +
                    '</div>' +
                    '</div>' +
                    '</td>' +
                    '<td>' +
                    '<div class="form-group">' +
                    '<div class="input-group date" id="datePickerMadeup' + currentRow + '">' +
                    '<input required="true" id="dateMadeup' + currentRow + '" path="entries[' + currentRow + '].dateMadeUp" cssClass="form-control" />' +
                    '<span class="input-group-addon" style="display: inline;"> <span class="glyphicon glyphicon-calendar"></span>' +
                    '</span>' +
                    '</div>' +
                    '</div>' +
                    '</td>' +
                    '<td><input required="true" path="entries[' + currentRow + '].minutesMadeUp" cssClass="form-control" size="5" /></td>' +
                    '<td><input required="true" path="entries[' + currentRow + '].projectDescription" cssClass="form-control" size="5" /></td>' +
                    '<td><a id="delete-' + currentRow + '">Delete</a></td>' +
                    '</tr>';
        }
        function hideRow(index) {
            console.log("test" + index);
            $('#row-' + index).hide();
        }
        $(function() {
            $('#addMakeupBtn').click(function() {
                $('#makeupTableBody')
                        .append(generateRow(largestMakeUpIndex));
                $('#delete-' + largestMakeUpIndex).click(function() {
                    rowId = $(this).attr('id').split('-')[1];
                    $('#row-' + rowId).hide();
                });
                $('#delete-' + largestMakeUpIndex)
                setLatestIndex(largestMakeUpIndex+1);
                var datePicker = $('.date');
                datePicker.datepicker({
                    autoclose: true
                });
            });

            $('#currentDate').on("change", function(){
                var dateChange = $("<input>").attr("type", "hidden").attr("name", "changeDate");
                $(".sectionTable").hide();
                $("#waitLoading").show();
                $("#sectionSelect").append($(dateChange));
                $("#sectionSelect").submit();
            });
        });

        var largestMakeUpIndex = 0;
        function setLatestIndex(latestIndex) {
            largestMakeUpIndex = latestIndex;
        }
    </script>
  <title>Student Makeup</title>
</head>

<body>

  <a id="backToAttendanceSummary" href="${context}/attendanceSummary/${sectionId}">Back to Attendance Summary</a>

  <br/><br/>
  <style>
    th, td {
      padding: 10px;
    }
  </style>
  <table>
    <tr><td align="right">Name:</td><td>${student.name}</td></tr>
    <tr><td align="right">WID:</td><td>${student.sisUserId}</td></tr>
  </table>
  
  <br/>
  <form:form id="makeupForm" modelAttribute="makeupForm" method="POST" action="${context}/studentMakeup/save">
    <c:if test="${not empty error}">
    <div class="alert alert-info">
        <p>${error}</p>
    </div>
    </c:if>
  
        <form:input type="hidden" id="sectionId" path="sectionId" />
        <form:input type="hidden" id="studentId" path="studentId" />

		<table class="table table-bordered">
			<thead>
				<tr>
					<th>Class Date</th>
					<th>Date Made Up</th>
					<th>Minutes Made Up</th>
					<th>Project Description</th>
					<th><input id="addMakeupBtn" class="hovering-purple-button" name="addMakeup" value="Add Makeup" /></th>
				</tr>
			</thead>

			<tbody id="makeupTableBody">
				<c:forEach items="${makeupForm.entries}" var="makeup" varStatus="makeupLoop">
					<tr>
						<td>
						    <form:input type="hidden" id="id${makeupLoop.index}" path="entries[${makeupLoop.index}].makeupId" />
                            <div class="form-group">
                                <div class="input-group date" id="datePickerDateOfClass${makeupLoop.index}">
                                    <form:input id="classDate${makeupLoop.index}" path="entries[${makeupLoop.index}].dateOfClass" cssClass="form-control" />
                                    <span class="input-group-addon" style="display: inline;"> <span class="glyphicon glyphicon-calendar"></span>
                                    </span>
                                </div>
                            </div>
						</td>
						<td>
							<div class="form-group">
								<div class="input-group date" id="datePickerMadeup-${makeupLoop.index}">
									<form:input id="dateMadeup${makeupLoop.index}" path="entries[${makeupLoop.index}].dateMadeUp" cssClass="form-control" />
									<span class="input-group-addon" style="display: inline;"> <span class="glyphicon glyphicon-calendar"></span>
									</span>
								</div>
							</div>
						</td>
						<td><form:input path="entries[${makeupLoop.index}].minutesMadeUp" cssClass="form-control" size="5" /></td>
                        <td><form:input path="entries[${makeupLoop.index}].projectDescription" cssClass="form-control" size="5" /></td>
						<td><a href="${context}/deleteMakeup/${makeupForm.sectionId}/${makeupForm.studentId}/${makeupForm.entries[makeupLoop.index].makeupId}">Delete</a></td>
					</tr>
                    <c:if test="${makeupLoop.last}">
                        <script type="text/javascript">
                            setLatestIndex(${makeupLoop.index});
                        </script>
                    </c:if>
				</c:forEach>
			</tbody>

		</table>

		<div>
			<input class="hovering-purple-button" type="submit" name="saveMakeup" value="Save Makeups" />
		</div>

	</form:form>
  <script src="${context}/js/moment.js"></script>
  <script src="${context}/bootstrap/js/bootstrap-datepicker.min.js"></script>
  <!-- Load Bootstrap JS -->
  <script src="${context}/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>