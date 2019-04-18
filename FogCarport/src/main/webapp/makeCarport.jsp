<%-- 
    Document   : makeCarport
    Created on : 12-04-2019, 16:39:35
    Author     : Malte
--%>

<jsp:include page='/header.jsp'></jsp:include>
<!-- Navigation -->
<nav class="navbar navbar-expand-lg navbar-light bg-light">
  <a class="navbar-brand" href="#">Fog Carport</a>
  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
    <span class="navbar-toggler-icon"></span>
  </button>

  <div class="collapse navbar-collapse" id="navbarSupportedContent">
    <ul class="navbar-nav mr-auto">
      <li class="nav-item">
        <a class="nav-link" href="partslist.jsp">Stykliste </a>
      </li>
      <li class="nav-item active">
        <a class="nav-link" href="makeCarport.jsp">Lav Carport <span class="sr-only">(current)</span></a>
      </li>
    </ul>
  </div>
</nav>
<!-- Navigation end -->

    <div class="d-flex justify-content-center" >
        <!-- Form start -->
        <form action="FrontController" method="post">
            
            <!-- Hidden input: &command=simpleorder -->
            <input type="hidden" name="command" value="simpleorder">
            
            <h1>Bestil Carport</h1>

            <!-- Height -->
            <div class="form-group">
                <label for="InputHeight">H�jde i cm</label>
                <input type="number" required class="form-control" id="InputHeight" placeholder="Indtast h�jde" name="height" min="200" max="300">
            </div>

            <!-- Length -->
            <div class="form-group">
                <label for="InputLength">L�ngde i cm</label>
                <input type="number" required class="form-control" id="InputLength" placeholder="Indtast l�ngde" name="length" min="240" max="720">
            </div>

            <!-- Width -->
            <div class="form-group">
                <label for="InputWidth">Bredde i cm</label>
                <input type="number" required class="form-control" id="InputWidth" placeholder="Indtast bredde" name="width" min="240" max="720">
            </div>

            <!-- Checkbox -->
            <div class="form-check">
                <input type="checkbox" class="form-check-input" id="CheckSkur" name="shed" value="y">
                <label class="form-check-label" for="CheckSkur">V�lg Skur</label>
            </div>

            <!-- Button to submit -->
            <button type="submit" class="btn btn-primary" style="margin-top: 5px;">Bestil Skur</button>
            
        </form>
        <!-- Form end -->
    </div>

<jsp:include page='/footer.jsp'></jsp:include>
