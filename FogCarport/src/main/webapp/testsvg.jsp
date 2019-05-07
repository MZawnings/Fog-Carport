<%-- 
    Document   : testsvg
    Created on : 07-05-2019, 15:35:30
    Author     : Camilla
--%>

<%@page import="logic.drawings.SVGDrawingRaisedRoof"%>	
<%@page import="data.models.OrderModel"%>	
<%	
    OrderModel order = new OrderModel();	
    order.setLength(7000);	
    order.setWidth(3600);	
    order.setIncline(20);	
    SVGDrawingRaisedRoof draw = new SVGDrawingRaisedRoof();	
    String drawing = draw.getRaisedRoofDrawing(order);	
    request.getSession().setAttribute("drawing", drawing);	
%>	

 <%@page contentType="text/html" pageEncoding="UTF-8"%>	
<!DOCTYPE html>	
<html>	
    <head>	
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">	
        <title>JSP Page</title>	
    </head>	
    <body>	
        ${ drawing }	
    </body>	
</html>
