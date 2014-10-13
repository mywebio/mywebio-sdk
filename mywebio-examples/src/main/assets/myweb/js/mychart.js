var chart = c3.generate({
	bindto: '#cpu',
    data: {
        x: 'x',
        rows: [
            ['x', 'user', 'system']
        ],
        types: {
            user: 'area',
            system: 'area'
        },
        groups: [['user', 'system']]
    },
    transition: {
        duration: 100
    },
    point: {
        show: false
    },
    tooltip: {
        show: false
    },
    grid: {
//		x: {
//            show: true
//        },
        y: {
            show: true
        }
    },
    axis: {
            x: {
                type: 'timeseries',
                height: 50,
                tick: {
                    rotate: 90,
                    format: function (t) { return new Date(t).toTimeString().substring(0,8) }
                }
            },
            y: {
                tick: {
					format: function (d) { return d+"%"; }
                },
                max: 99,
                min: 0
            },
    }
});


function gauge(name) {
	if (!document.getElementById("g"+name)) {
		$('#gauges').append("<div class='col-md-3 xgauge'><h2>"+name.replace('cpu','core ')+"</h2><div id='g"+name+"'></div></div>");
	}
	return c3.generate({
		bindto: "#g"+name,
		size: {
			height: 200,
			width: 250
		},
		data: {
			columns: [
				['data', 0.0]
				],
			type: 'gauge'
		},
		tooltip: {
			show: false
		},
		gauge: {
			label: {
				format: function(value, ratio) {
						return Math.round(value)+"%";
					},
				show: true
			}
		},
		color: {
			pattern: ['#60B044', '#F6C600', '#F97600', '#FF0000'],
			threshold: {
				values: [30, 60, 90, 100]
			}
		}
	});
}

var lastCpuStat = {};
var cpuData = {};
var gauges = {};

function updateChart(name, data) {
	if (!cpuData[name]) {
		cpuData[name]=[['x','user','system']];
	}
	len = cpuData[name].length;
	cpuData[name][len] = data;
	if (len > 360) cpuData[name].splice(1,300);
	chart.axis.range({max: {x: data[0]}, min: {x: (data[0]-60000) }});
	chart.load({ rows: cpuData[name] });
}

function updateGauge(name, data) {
	if (!gauges[name]) gauges[name] = gauge(name);
	total = data[1] + data[2];
	gauges[name].load({ columns: [['data', total]]});
}

function onCpuStatReceived(data, status, xhr) {
	if (status == "success") {
		$("#num_cores").replaceWith(data.available);
		for (name in data) {
			if (name.match("cpu.*")) {
				if (!lastCpuStat[name]) lastCpuStat[name]=data[name];
				cData = prepareCpuData(data.timestamp, data[name], lastCpuStat[name]);
				lastCpuStat[name] = data[name];
				if (name=="cpu") updateChart(name, cData);
				else updateGauge(name, cData);
			}
		}
		// show idle cores
		for (name in lastCpuStat) {
			if (!data[name]) {
				updateGauge(name, [data.timestamp,0,0]);
			}
		}
	}
}

function onCpuInfoReceived(data, status, xhr) {
	if (status == "success") {
		$("#cpu_arch").replaceWith(data.Processor);
		$("#cpu_model").replaceWith(data.Hardware);
		$("#bogomips").replaceWith(data.BogoMIPS);
	}
}

function prepareCpuData(timestamp, a, b) {
	total = ((a[0]+a[1]+a[2]+a[3]+a[4]+a[5]+a[6])-(b[0]+b[1]+b[2]+b[3]+b[4]+b[5]+b[6]));
	if (total==0) total = 1;
	user = ((a[0]+a[1])-(b[0]+b[1]))*100/total;
	if (user > 99) user = 99.0;
	system = ((a[2]+a[4]+a[5]+a[6])-(b[2]+b[4]+b[5]+b[6]))*100/total;
	if (system + user > 100) system = 100 - user;
	return [timestamp, user, system];
}

function updateCpuStat(timeout) {
	$.getJSON("/io.myweb.examples/stat", null, function(data,status,xhr) {onCpuStatReceived(data,status,xhr);});
}

function updateCpuInfo() {
	$.getJSON("/io.myweb.examples/cpuinfo", null, function(data,status,xhr) {onCpuInfoReceived(data,status,xhr);});
}

function fakeCpuData(timeout) {
	timestamp = new Date().getTime();
	lastCpuStat['cpu'] = [timestamp, Math.random()*90, Math.random()*10];
	updateChart('cpu', lastCpuStat['cpu']);
	updateGauge('cpu0', lastCpuStat['cpu']);
	updateGauge('cpu1', lastCpuStat['cpu']);
	updateGauge('cpu2', lastCpuStat['cpu']);
	updateGauge('cpu3', lastCpuStat['cpu']);
}

setInterval(updateCpuStat, 1000);
updateCpuInfo();
//setInterval(fakeCpuData, 1000);
//onCpuInfoReceived({"CPU_revision":"0","CPU_architecture":"7","Serial":"0000000000000000","Hardware":"UNKNOWN","CPU_variant":"0x1","CPU_part":"0x06f","Revision":"0003","BogoMIPS":"13.52","processor":"3","CPU_implementer":"0x51","Features":"swp half thumb fastmult vfp edsp thumbee neon vfpv3 tls vfpv4","Processor":"ARMv7 Processor rev 0 (v7l)"},
//		"success",null);