var network = null;

function destroyNetwork() {
    if (network !== null) {
        network.destroy();
        network = null;
    }
}

function upload() {
    var file = document.getElementById('fileInput').files[0];

    const fd = new FormData(), ajax = new XMLHttpRequest();

    fd.append('file', file, file.name);

    ajax.open('POST', '/upload');
    ajax.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

    ajax.onreadystatechange = () => {
        let text = "Upload status: " + ajax.status;
        console.log(text);
    };

    ajax.send(fd);
}

function drawNetwork() {
    const ajax = new XMLHttpRequest();
    var regionalWhAmount = document.getElementById('regionalWhAmount').value;
    ajax.open('GET', '/getResult?regionalWhAmount=' + regionalWhAmount);
    ajax.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

    ajax.addEventListener("readystatechange", () => {
        let text = ajax.responseText;
        const response = JSON.parse(text);
        destroyNetwork();
        var data = buildNetwork(response.citiesRoadsDto.cities, response.citiesRoadsDto.roads);

        var container = document.getElementById('mynetwork');
        var parentRect = container.parentNode.getBoundingClientRect();

        var options = {
            autoResize: false,
            // width: parentRect.width + "px",
            // height: parentRect.height + "px"
            nodes: {
                shape: 'dot'
            },
            edges: {
                smooth: false
            },
            physics: false,
            interaction: {
                dragNodes: false,
                zoomView: true,
                dragView: true
            }
        };

        network = new vis.Network(container, data, options);

        // // add event listeners
        // network.on('select', function (params) {
        //     document.getElementById('selection').innerHTML = 'Selection: ' + params.nodes;
        // });

        // ensure
        window.addEventListener('resize', function(event) {
            network.setOptions({
                width: parentRect.width + "px",
                height: parentRect.height + "px"
            })
        });
    });

    ajax.send();
}

function buildNetwork(cities, roads) {
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

        if (type === 'SUPPLIER') {
            nodes.add({
                id: i,
                label: String(city),
                x: 0,
                y: suppCount * 100
            });
            suppCount++;
        }
        if (type === 'NATIONAL') {
            nodes.add({
                id: i,
                label: String(city),
                x: 300,
                y: natCount * 100
            });
            natCount++;
        }
        if (type === 'REGIONAL') {
            nodes.add({
                id: i,
                label: String(city),
                x: 600,
                y: regCount * 100
            });
            regCount++;
        }
        if (type === 'LOCAL') {
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

    function push(from, to) {
        edges.push({
            from: from,
            to: to
        });
        connectionCount[to]++;
        connectionCount[from]++;
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

        push(supplierId, nationalId);
        push(nationalId, regionalId);
        push(regionalId, localId);
    }

    return {nodes: nodes, edges: edges};
}