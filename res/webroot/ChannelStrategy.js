class ChannelStrategy{
	constructor(observer, node){
		this.observer = observer;
		this.node = node;
	}

	onOpen(channel){
		console.log("Open a new P2P connection");
		this.observer.notifyConnectionComplete();
		channel.send(JSON.stringify({"type": "hello", "id": this.node.id}))
	}

	onMessage(msg, channel){
		console.log("A messge from p2p channel " + channel.id + ":", JSON.parse(msg.data));
		let json = JSON.parse(msg.data);
		if(json.type == "hello"){
			console.log("Hello type");
			this.node.addConnectedNode(channel.id, json.id);
		}else if(json.type == "movementTo"){
			if(this.node.isOutOfRadius(json.x, json.y)){
				console.log("out of radius");
				this.node.removeConnectedNode(channel.id);
				channel.close();
			}
		}
	}

	onClose(channel){
		console.log("Close a P2P connection"); 
		this.node.removeConnectedNode(channel.id);
	}

	onDisconnected(channel){
		console.log("disconnected");
		this.node.removeConnectedNode(channel.id);
	}

}