const serverURL = 'ws://localhost:8081';

let webSocket;
let ctx;

let nodes = new Map();

window.onload = function(){
    webSocket = new WebSocket(serverURL);
    setUpSocket(webSocket);

    document.getElementById("conf1").onclick = () => {
        let numNodes = document.getElementById("in_conf1_numNodes").value;
        webSocket.send(getConfiguration1(numNodes))
    };


    document.getElementById("conf2").onclick = () => {
        let numNodes = document.getElementById("in_conf2_numNodes").value;
        webSocket.send(getConfiguration2(numNodes))
    };

    document.getElementById("move").onclick = () => {
        let id = document.getElementById("in_move_id").value;
        let x = document.getElementById("in_move_x").value;
        let y = document.getElementById("in_move_y").value;
        webSocket.send(move(id,x,y));
    };

    document.getElementById("disconnect").onclick = () => {
        let id = document.getElementById("in_disconnect_id").value;
        webSocket.send(JSON.stringify(
            {"type": "disconnect_node",
             "id": id}));
    };

    document.getElementById("modify").onclick = () => {
        let id = document.getElementById("in_modify_id").value;
        let name = document.getElementById("in_modify_name").value;
        let value = document.getElementById("in_modify_value").value;
        webSocket.send(modifyA(id,name,value));
        webSocket.send(modifyS(id,name,value));
    };

    var c = document.getElementById("myCanvas");
    ctx = c.getContext("2d");
}

function makeCircle(x, y, id){
    let radius = 8;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    ctx.fillStyle = "gray";
    ctx.fill();
    ctx.stroke();

    ctx.font = "15px Arial";
    ctx.fillText(id, x-radius-3, y-radius-3);
}

function draw(){
    ctx.clearRect(0, 0, 500, 300);
    let keys = Array.from( nodes.keys());
    for(let i = 0; i < nodes.size; i++){
        let obj = nodes.get(keys[i]);
        makeCircle(obj.x + 100, obj.y +100, obj.id);
        //console.log("len", obj.ids.length);
        for(let j = 0; j < obj.ids.length; j++){
            let destObj = nodes.get(obj.ids[j]);
            //console.log("destObj", destObj);
            ctx.beginPath();
            ctx.moveTo(obj.x + 100, obj.y+ 100);
            ctx.lineTo(destObj.x+ 100, destObj.y+ 100);
            ctx.strokeStyle = "gray";
            ctx.stroke();
        }
    }
    
}

function setUpSocket(webSocket){
	webSocket.onopen = () => {
            console.log('open');
            //ask for node setup
            webSocket.send(JSON.stringify({"type": "manager_setup_demand"}));
    }
	webSocket.onclose = () => console.log('close');
	webSocket.onerror = (error) => console.log('error', error.message);
	webSocket.onmessage = (msg) => {
        console.log('message: ', msg.data);
        try{
			let json = JSON.parse(msg.data);
			if(json.type != null){
                switch(json.type){
                    case "node_state":
                       // console.log("valuse: ", nodes.values());
                        if(typeof json.x !== 'undefined'&& typeof json.y !== 'undefined'){
                            if(nodes.get(json.id)){
                                nodes.get(json.id).x = parseInt(json.x);
                                nodes.get(json.id).y = parseInt(json.y);
                            }else{
                                nodes.set(json.id, {x : json.x, y : json.y, id : json.id});
                            }
                        }
                    
                        nodes.get(json.id).ids = json.connected_nodes_id;
                        
                        draw();
                        break;
                    case "node_disconnection":
                        nodes.delete(json.id);
                        draw();
                        break;
                    case "nodes_configurations":
                        break;
                    default:
                        console.error("Incorrect message type", json.type);
                }
			}else if(json.desc || json.candidate){
                connectionsManager.elaborateMsg(json);
            }else{
                console.error("Message unrecognized", json);
            } 
		}catch(err){
			console.error(err);
		}
    };
}



function getConfiguration1(num){
    return JSON.stringify({"type": "set_configuration",
            "configuration":  
                {"type": "node_configuration",
                    "node_name": "Thermometer with Led",
                    "sensors": [
                        {"sensor_name": "Thermometer",
                            "value_name": "Temperature",
                            "value_type": "real",
                        }
                    ],
                    "actuators": [
                        {"actuator_name": "Led",
                            "value_name": "Is turn on",
                            "value_type": "boolean"
                        }
                    ],
                    "radius": 80 
                },
        "node_quantity": num, 
        "cols": 2, 
        "stepX": 40, 
        "stepY": 40 
    })
}


function getConfiguration2(num){
    return JSON.stringify({"type": "set_configuration",
            "configuration":  
                {"type": "node_configuration",
                    "node_name": "Car counter and Motion detector",
                    "sensors": [
                        {"sensor_name": "Motion detector",
                            "value_name": "Motion detect",
                            "value_type": "boolean",
                        },
                        {"sensor_name": "Car counter",
                            "value_name": "Number of cars",
                            "value_type": "natural",
                        }
                    ],
                    "actuators": [],
                    "radius": 160
                },
        "node_quantity": num, 
        "cols": 4, 
        "stepX": 80, 
        "stepY": 80 
    })
}

function move(id, x, y){
    return JSON.stringify({"type": "move_node",
	"id": id,
	"x": x,
	"y": y })
}

function modifyA(id,name,value){
    return JSON.stringify(
        {"type": "change_node_state",
        "id": id,
        "device_type": "actuator",
        "actuator_name": name,
        "value": value}
    );
}

function modifyS(id,name,value){
    return JSON.stringify(
        {"type": "change_node_state",
        "id": id,
        "device_type": "sensor",
        "actuator_name": name,
        "value": value}
    );
}