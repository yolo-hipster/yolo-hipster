Raphael.fn.pieChart = function (cx, cy, r, values, labels, stroke) {
    var paper = this,
    rad = Math.PI / 180,
    chart = this.set();
    function sector(cx, cy, r, startAngle, endAngle, params) {
        var x1 = cx + r * Math.cos(-startAngle * rad),
        x2 = cx + r * Math.cos(-endAngle * rad),
        y1 = cy + r * Math.sin(-startAngle * rad),
        y2 = cy + r * Math.sin(-endAngle * rad);
        return paper.path(["M", cx, cy, "L", x1, y1, "A", r, r, 0, +(endAngle - startAngle > 180), 0, x2, y2, "z"]).attr(params);
    }
    var angle = 0,
    total = 0,
    start = 0,
    process = function (j) {
        var value = values[j],
        angleplus = 360 * value / total,
        popangle = angle + (angleplus / 2),
        color = Raphael.hsb(start, .75, 1),
        ms = 100,
        delta = 30,
        bcolor = Raphael.hsb(start, 1, 1),
        p = sector(cx, cy, r, angle, angle + angleplus, {
            fill: "90-" + bcolor + "-" + color, 
            stroke: stroke, 
            "stroke-width": 1
        });
        
        $(".user"+j).mouseover(function () {
            $(".user"+j).css("color", "#CCC");
            p.stop().animate({
                transform: "s1.1 1.1 " + cx + " " + cy
            }, ms, "easein");
            
        }).mouseout(function () {
            $(".user"+j).css("color", "#000");
            p.stop().animate({
                transform: ""
            }, ms, "easein");
            
        });
        
        p.mouseover(function () {
            $(".user"+j).css("color", "#CCC");
            p.stop().animate({
                transform: "s1.1 1.1 " + cx + " " + cy
            }, ms, "easein");
            
        }).mouseout(function () {
            $(".user"+j).css("color", "#000");
            p.stop().animate({
                transform: ""
            }, ms, "easein");
            
        });
        angle += angleplus;
        chart.push(p);
        start += .1;
    };
    for (var i = 0, ii = values.length; i < ii; i++) {
        total += values[i];
    }
    for (i = 0; i < ii; i++) {
        process(i);
    }
    return chart;
};

$(function () {
    
    
    $('#envoyer').click(function(){
        
        $.ajax({
            url: "quiEdit.php?url="+$("#url").val()
        }).done(function ( data ) {
            $("#result").html(data);
            var values = [],
    labels = [];
    $("tr").each(function () {
        values.push(parseInt($("td", this).text(), 10));
        labels.push($("th", this).text());
    });
    $("table").hide();
            Raphael("holder", 620, 620).pieChart(310, 220, 200, values, labels, "#fff");
            $("#result").fadeIn(500);
        });
        
            
    
       
    
    });
    
});
