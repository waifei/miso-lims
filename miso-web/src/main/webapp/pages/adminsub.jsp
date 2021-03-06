<%--
  ~ Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
  ~ MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
  ~ **********************************************************************
  ~
  ~ This file is part of MISO.
  ~
  ~ MISO is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MISO is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MISO.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ **********************************************************************
  --%>

<div id="subcontent">
	<h2>Navigation</h2>
	<br />
	<%--<h2>Search</h2>--%>
	<%--<span class="inline">--%>
	<%--<form action="/miso/search">--%>
	<%--<input type="text" id="search" value="Search for..." onfocus="clearInputField(this);"/><input value="Search"--%>
	<%--type="submit"/>--%>
	<%--</form>--%>
	<%--</span><br/>--%>

	<h2>Sample Processing</h2>
	<ul class="bullets">
		<li><a href="<c:url value="/miso/sample/receipt"/>">Receive
				Samples</a></li>
		<li><a href="<c:url value="/miso/importexport"/>">Import &amp;
				Export</a></li>
		<%--<li><a href="<c:url value="/miso/plate/import"/>">Import Plate Sheet</a></li>--%>
		<%--<li><a href="<c:url value="/miso/plate/export"/>">Export Plate Sheet</a></li>--%>
	</ul>

	<h2>Sequencing</h2>
	<ul class="bullets">
		<li><a href="<c:url value="/miso/pools/ready"/>">Ready to Run</a></li>
		<li><a href="<c:url value="/miso/container/new"/>">Create New
				Partition Container</a></li>
		<li><a href="<c:url value="/miso/run/new"/>">Create New Run</a></li>
	</ul>

    <h2>Tracking</h2>
    <ul class="bullets">
        <li><a href="<c:url value="/miso/runs"/>">List Runs</a></li>
        <li><a href="<c:url value="/miso/containers"/>">List Partition Containers</a></li>
        <li><a href="<c:url value="/miso/experiments"/>">List Experiments</a></li>
        <li><a href="<c:url value="/miso/pools"/>">List Pools</a></li>
        <li><a href="<c:url value="/miso/poolorders/outstanding"/>">List Outstanding Orders</a></li>
        <li><a href="<c:url value="/miso/libraries"/>">List Libraries</a></li>
        <li><a href="<c:url value="/miso/samples"/>">List Samples</a></li>
        <li><a href="<c:url value="/miso/studies"/>">List Studies</a></li>
        <li><a href="<c:url value="/miso/kitdescriptors"/>">List Consumables</a></li>
        <li><a href="<c:url value="/miso/plates"/>">List Plates</a></li>
        <li><a href="<c:url value="/miso/boxes"/>">List Boxes</a></li>
        <li><a href="<c:url value="/miso/sequencers"/>">List Sequencers</a></li>
        <li><a href="<c:url value="/miso/tagbarcodes"/>">List Tag Barcodes</a></li>
    </ul>

	<h2>Print Jobs</h2>
	<ul class="bullets">
		<li><a href="<c:url value="/miso/printjobs"/>">My Print Jobs</a></li>
		<li><a href="<c:url value="/miso/custombarcode"/>">Custom
				Barcode Printing</a></li>
		<sec:authorize access="hasRole('ROLE_ADMIN')">
			<li><a
				href="<c:url value="/miso/admin/configuration/printers"/>">Printers</a></li>
		</sec:authorize>
	</ul>

	<sec:authorize access="hasRole('ROLE_ADMIN')">
		<h2>User Administration</h2>
		<ul class="bullets">
			<li><a href="<c:url value="/miso/admin/users"/>">List Users</a></li>
			<li><a href="<c:url value="/miso/admin/user/new"/>">Create
					User</a></li>
		</ul>

		<h2>Group Administration</h2>
		<ul class="bullets">
			<li><a href="<c:url value="/miso/admin/groups"/>">List
					Groups</a></li>
			<li><a href="<c:url value="/miso/admin/group/new"/>">Create
					Group</a></li>
		</ul>
	</sec:authorize>
</div>
