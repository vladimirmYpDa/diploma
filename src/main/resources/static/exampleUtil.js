/**
 * Created by Alex on 5/20/2015.
 */

function loadJSON(path, success, error) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                success(JSON.parse(xhr.responseText));
            } else {
                error(xhr);
            }
        }
    };
    xhr.open('GET', path, true);
    xhr.send();
}

function func(cities, roads) {
    var nodes = new vis.DataSet();
    var edges = [];
    var connectionCount = [];

    var citiesMap = new Map();

    var i = 0;
    var suppCount = 0;
    var natCount = 0;
    var regCount = 0;
    var locCount = 0;

    for (var city in cities) {
        var type = cities[city];

        if (type === 'Sup') {
            nodes.add({
                id: i,
                label: String(city),
                x: 0,
                y: suppCount * 100
            });
            suppCount++;
        }
        if (type === 'Nat') {
            nodes.add({
                id: i,
                label: String(city),
                x: 300,
                y: natCount * 100
            });
            natCount++;
        }
        if (type === 'Reg') {
            nodes.add({
                id: i,
                label: String(city),
                x: 600,
                y: regCount * 100
            });
            regCount++;
        }
        if (type === 'Loc') {
            nodes.add({
                id: i,
                label: String(city),
                x: 900,
                y: locCount * 100
            });
            locCount++;
        }
        citiesMap[city] = i++;
    }

    for (var k = 0; k < roads.length; k++) {
        let road = roads[k];
        var localCity = road.localToRegionalConn.destinationNode.name;
        var regionalCity = road.localToRegionalConn.sourceNode.name;
        var nationalCity = road.regionalToNationalConn.sourceNode.name;
        var supplierCity = road.nationalToSupplierConn.sourceNode.name;

        let regionalId = citiesMap[regionalCity];
        let localId = citiesMap[localCity];
        let nationalId = citiesMap[nationalCity];
        let supplierId = citiesMap[supplierCity];

        edges.push({
            from: supplierId,
            to: nationalId
        });
        connectionCount[nationalId]++;
        connectionCount[supplierId]++;

        edges.push({
            from: nationalId,
            to: regionalId
        });

        connectionCount[regionalId]++;
        connectionCount[nationalId]++;

        edges.push({
            from: regionalId,
            to: localId
        });
        connectionCount[localId]++;
        connectionCount[regionalId]++;
    }

    return {nodes: nodes, edges: edges};
}


function getScaleFreeNetwork(nodeCount) {
    var nodes = [];
    var edges = [];
    var connectionCount = [];

    var cities = ['Шан-хай', 'Москва', 'Киев'];

    for (var k = 0; k < suppliers.length; k++) {
        nodes.push({
            id: k,
            label: String(suppliers[k])
        });
    }

    var suppliers = new Map();
    suppliers.put(1, 'Шан-хай')
    suppliers.put(2, 'Москва')

    var national = new Map();
    national.put(1, 'Киев');
    national.put(2, 'Одесса');

    var local = new Map();
    local.put(1, 'Донецк');
    local.put(2, 'Харьков');


    // randomly create some nodes and edges
    for (var i = 0; i < nodeCount; i++) {


        connectionCount[i] = 0;

        // create edges in a scale-free-network way
        if (i == 1) {
            var from = i;
            var to = 0;
            edges.push({
                from: from,
                to: to
            });
            connectionCount[from]++;
            connectionCount[to]++;
        } else if (i > 1) {
            // var conn = edges.length * 2;
            // var rand = Math.floor(Math.random() * conn);
            // var cum = 0;
            // var j = 0;
            // while (j < connectionCount.length && cum < rand) {
            //     cum += connectionCount[j];
            //     j++;
            // }

            // console.log(conn +" conn "
            //     + rand +" rand " + cum + " cum " + j + " j " + i + " i ");

            var from = i;
            var to = 1;
            edges.push({
                from: from,
                to: to
            });
            connectionCount[from]++;
            connectionCount[to]++;
        }
    }

    return {nodes: nodes, edges: edges};
}

var randomSeed = 764; // Math.round(Math.random()*1000);
function seededRandom() {
    var x = Math.sin(randomSeed++) * 10000;
    return x - Math.floor(x);
}

function getScaleFreeNetworkSeeded(nodeCount, seed) {
    if (seed) {
        randomSeed = Number(seed);
    }
    var nodes = [];
    var edges = [];
    var connectionCount = [];
    var edgesId = 0;


    // randomly create some nodes and edges
    for (var i = 0; i < nodeCount; i++) {
        nodes.push({
            id: i,
            label: String(i)
        });

        connectionCount[i] = 0;

        // create edges in a scale-free-network way
        if (i == 1) {
            var from = i;
            var to = 0;
            edges.push({
                id: edgesId++,
                from: from,
                to: to
            });
            connectionCount[from]++;
            connectionCount[to]++;
        } else if (i > 1) {
            var conn = edges.length * 2;
            var rand = Math.floor(seededRandom() * conn);
            var cum = 0;
            var j = 0;
            while (j < connectionCount.length && cum < rand) {
                cum += connectionCount[j];
                j++;
            }


            var from = i;
            var to = j;
            edges.push({
                id: edgesId++,
                from: from,
                to: to
            });
            connectionCount[from]++;
            connectionCount[to]++;
        }
    }

    return {nodes: nodes, edges: edges};
}