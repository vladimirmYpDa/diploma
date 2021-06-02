var network = null;

function destroyNetwork() {
    if (network !== null) {
        network.destroy();
        network = null;
    }
}

function fetchNetwork() {
    const ajax = new XMLHttpRequest();
    var regionalWhAmount = document.getElementById('regionalWhAmount').value;
    var transportPrice = document.getElementById('transportPrice').value;
    ajax.open('GET', '/getResult?regionalWhAmount=' + regionalWhAmount + '&transportPrice=' + transportPrice);
    ajax.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

    ajax.addEventListener("readystatechange", () => {
        let text = ajax.responseText;
        const response = JSON.parse(text);
        destroyNetwork();
        modifyDom(response);
        var data = buildNetwork(response.roads);

        var container = document.getElementById('mynetwork');
        var parentRect = container.parentNode.getBoundingClientRect();

        var options = {
            autoResize: true,
            // width: parentRect.width + "px",
            // height: parentRect.height + "px"
            nodes: {
                shape: 'dot'
            },
            edges: {
                smooth: false
            },
            physics: {
                enabled: true,
                minVelocity: 1,
                stabilization: {
                    enabled: true,
                    iterations: 1000
                }
            },
            layout: {
                randomSeed: 1,
                improvedLayout: false
            },
            interaction: {
                dragNodes: false,
                zoomView: true,
                dragView: true
            }
        };

        network = new vis.Network(container, data, options);
        network.on("stabilizationIterationsDone", function() { network.stopSimulation()})
        // // add event listeners
        // network.on('select', function (params) {
        //     document.getElementById('selection').innerHTML = 'Selection: ' + params.nodes;
        // });

        // ensure
        // window.addEventListener('resize', function(event) {
        //     network.setOptions({
        //         width: parentRect.width + "px",
        //         height: parentRect.height + "px"
        //     })
        // });
    });

    ajax.send();
}

function modifyDom(result) {
    const regionalTable = document.getElementById("regionalTable");
    const downloadLink = document.getElementById('downloadLink');
    const sum = document.getElementById("sum");
    const sumToNat = document.getElementById("sumToNat");
    const sumToReg = document.getElementById("sumToReg");

    downloadLink.setAttribute('href', '/' + result.downloadFilename);

    sum.setAttribute("value", result.sumConnection);
    sumToNat.setAttribute("value", result.sumToNationalConnection);
    sumToReg.setAttribute("value", result.sumToRegionalConnection);

    regionalTable
    let newTable = document.createElement('div');
    newTable.id = "regionalTable";

    regionalTable.parentNode.replaceChild(newTable, regionalTable);
    let rt = document.createElement('table');
    rt.setAttribute("class", "table")
    newTable.appendChild(rt);

    let headerRow = rt.createTHead().insertRow();
    headerRow.insertCell(0).innerHTML = "Рег. узел";
    headerRow.insertCell(1).innerHTML = "Трансп. затр.";
    
    for (const [key, value] of Object.entries(result.regionalSums)) {
        let row = rt.insertRow();
        row.insertCell(0).innerHTML = key;
        row.insertCell(1).innerHTML = value;
    }
}

function parseNode(node) {
    var displayTooltip, size, fontSize, color;
    switch (node.nodeType) {
        case 'SUPPLIER':
            size = 24;
            fontSize = 16;
            color = '#FF9999';
            break;
        case 'NATIONAL':
            size = 18;
            fontSize = 14;
            color = '#FFFF99';
            displayTooltip = 'Национальный узел';
            break;
        case 'REGIONAL':
            size = 12;
            fontSize = 12;
            color = '#99FF99';
            displayTooltip = 'Региональный узел';
            break;
        case 'LOCAL':
            size = 6;
            fontSize = 10;
            color = '#99FFFF';
            displayTooltip = 'Узел поставщика\nСпрос:' + node.demand;
            break;
    }

    return {
        id: node.id,
        label: node.name,
        title: displayTooltip,
        size: size,
        color: color,
        font: {
            size: fontSize
        }
    };
}

function buildNetwork(roads) {
    var netNodes = new vis.DataSet();
    var edges = [];
    var nodes = new Map();

    function storeNode(node) {
        nodes[node.id] = node;
        netNodes.update(parseNode(node));
        return nodes[node.id];
    }

    function storeConnection(conn) {
        storeNode(conn.sourceNode);
        storeNode(conn.destinationNode);

        edges.push({
            from: conn.sourceNode.id,
            to: conn.destinationNode.id,
            length: (conn.distance / 2) + 10,
            title: conn.distance + " км"
        });
    }

    for (var roadId in roads) {
        let road = roads[roadId];

        storeConnection(road.localToRegionalConn);
        storeConnection(road.regionalToNationalConn);
        storeConnection(road.nationalToSupplierConn);
    }

    return {nodes: netNodes, edges: edges};
}