// MasterFreeze / (c) Robert van Heumen 2013 / 2014
// Program to control a bunch of MasterFreezes / in progress

// Uncurrent colors:
//	Color.new255(231,185,106);
//	Color.new255(31,152,135);

(
s.waitForBoot({

	var version = "0.4";
	var pID = 0;
	var cfg = (
		project: ["PJ"],
		name: ["   PJ"],
		filePath: ["/Users/robert/---data---/audio/samples/voice/pjharvey1.aif"],
		freezeRow: [["LOW","HIGH FADE","LOW","LOOP","PULSE"]]
	);
	var filePath = cfg.filePath[pID];
	var freezeInstance = Array.newClear(10);

	var wMF, drawDone = 0, backDrop, bFile, tFilePath,tHeader, buffer, cRecord, pRecord, tRecord, tChannels, backBox, bRecord;
	var penColorGrey = Color.grey(rrand(0,0.5), rrand(0,150));
	var penColorYellow = Color.new255(231,185,106, rrand(0,150));
	var filePathShort = subStr(filePath, filePath.findBackwards("/",offset: max(0,(filePath.size - 30))), filePath.size);

	if(filePathShort.size < filePath.size, {filePathShort = "..."++filePathShort});
	filePathShort = " "++filePathShort;

	buffer = Buffer.alloc(s, s.sampleRate*60,2);
	buffer.read(filePath);

	wMF = Window(cfg.project[pID]++"Freeze" + version, Rect(20,300,600,630),false)
	.background = Color.grey(0.9)
	.alpha = 0.9;

	backDrop = UserView(wMF,Rect(0,0,600,630))
	.mouseDownAction_({arg me; drawDone = 0; me.refresh});
	backDrop.drawFunc = {
		if(drawDone == 0, {
			rrand(40,100).do{|i|
				Pen.color = [penColorGrey,penColorYellow].wchoose([0.9,0.1]);
				Pen.addRect(
					Rect((wMF.bounds.width).rand, -2, 20, (wMF.bounds.height + 4))
				);
				Pen.perform(\stroke);
			};
			drawDone = 1;
		})
	};
	backDrop.clearOnRefresh_(true);

	// header
	tHeader = StaticText(wMF,Rect(30,-30,540,180))
	.string_(cfg.name[pID]++"Freeze")
	.font_(Font("Bickham Script Pro",150))
	.alpha_(0);

	// sound file selection
	bFile = SmoothButton(wMF, Rect(30,160,140,15))
	.radius_(2)
	.border_(1)
	.font_(Font("Helvetica",10))
	.states_([["Choose soundfile",Color.black,Color.white]])
	.canFocus_(false)
	.action_({
		Dialog.getPaths(
			{
				arg path;
				filePath = path[0];
				filePath.postln;
				buffer.read(filePath.asString);
				filePathShort = subStr(filePath, filePath.findBackwards("/",offset: max(0,(filePath.size - 30))), filePath.size);
				if(filePathShort.size < filePath.size, {filePathShort = "..."++filePathShort});
				filePathShort = " "++filePathShort;
				tFilePath.string = filePathShort;
			},
			{"Geen bestanden geselecteerd.".postln},
			false
		);
	});
	tFilePath = StaticText(wMF,Rect(180,160,390,15))
	.font_(Font("Helvetica",10))
	.background_(Color.grey(1,0.8))
	.string_(filePathShort);

	backBox = UserView(wMF, Rect(180,185,135,15))
		.background_(Color.grey(1,0.8))
		.canFocus_(false);

	tRecord = StaticText(wMF, Rect(190, 183, 60, 20))
		.font_(Font("Helvetica",10))
		.string_("Record");

	pRecord = PopUpMenu(wMF, Rect(225, 185, 35, 16))
		.items_(["2","4","6","8"])
		.font_(Font("Helvetica",10))
		.action_({
			s.recChannels_(pRecord.item.asInt); ("NumChannels for recording set to"+pRecord.item).postln;
			});

	tChannels = StaticText(wMF, Rect(265, 183, 80, 20))
		.font_(Font("Helvetica",10))
		.string_("channels");

	bRecord = (SmoothButton(wMF, Rect(330, 185, 50, 15))
		.border_(1)
		.radius_(2)
		.font_(Font("Helvetica",10))
		.canFocus_(false)
		.states_([["Start",Color.black, Color.white],["Stop",Color.black, Color.red]])
		.action_({
			if(bRecord.value==1, {s.record}, {s.stopRecording});
		})
	);

	// freeze rows
	case
	{pID == 0} // PJFreeze
	{
		freezeInstance[0] = MasterFreeze.new(cfg.freezeRow[pID][0], buffer, win: wMF, guiTop: 230, initWait:1,
			filterFade: 20, filterAttack: 10, pitchFactor: 0.2, waitArray: [10,10,20,20,5,5,10,20], freezeType:1, gainFactor: -3);
		freezeInstance[1] = MasterFreeze.new(cfg.freezeRow[pID][1], buffer, win: wMF, guiTop: 230, guiIndex: 2, initWait:2, filterFade: 20,
			filterAttack: 10, pitchFactor: 2, waitArray: [20,10,20,10,5,5,10,20], freezeType:2, gainFactor: -12);
		freezeInstance[2] = MasterFreeze.new(cfg.freezeRow[pID][2], buffer, win: wMF, guiTop: 230, guiIndex: 3, initWait:1, filterFade: 20,
			filterAttack: 10, pitchFactor: 0.3, waitArray: [10,10,20,20,5,5,10,20], freezeType:1, gainFactor: -3);
		freezeInstance[3] = MasterFreeze.new(cfg.freezeRow[pID][3], buffer, win: wMF, guiTop: 230, guiIndex: 4, initWait:1, filterFade: 20,
			filterAttack: 10, pitchFactor: 1, waitArray: [15,20,20,10,5,5,10,20], openArray: [1,1,1,1,1,1,1,1], freezeType:3, gainFactor: -12,
			output: [2,3]);
		freezeInstance[4] = MasterFreeze.new(cfg.freezeRow[pID][4], buffer, win: wMF, guiTop: 230, guiIndex: 5, initWait:1, filterFade: 20,
			filterAttack: 10, pitchFactor: 0.4, waitArray: [20,20,20,10,5,5,10,20], openArray: [1,1,1,1,1,1,1,1], freezeType:4, gainFactor: -6);
	}
	;

	//	arg 	name, 			// name of the instance
	//		buf, 				// buffer
	//		waitArray = [10,10,20], 		// array with wait times between freezes (seconds); this also sets the total length
	//		openArray, 		// array with open times of the freeze gate (seconds)
	//		dieOutArray, 		// array with dieOut values to be sent with every freeze
	//		filterAttack = 0.5, 	// attack for LP filter in freezeType 1,3 & 4 (seconds)
	//		filterFade = 30, 	// release for LP filter in freezeType 1,2 & 3 (seconds)
	//		initWait = 0,		// initial wait before freeze kicks in (seconds)
	//		endWait = 10,		// extra time at the end to allow the processing to develop (seconds)
	//	 	startPos = 2,		// startposition for playback sample (seconds)
	//		output = [0,1],		// output channel, stereo
	//	 	subOut = 8,		// output channel for sub
	//		countDisp = 1, 		// display count, 0=NO 1=YES
	//		freezeType = 1, 	// freeze function type, 1=short delay continous 2=longer delay fading in/out, 3=repeats, 4=pulse
	//		pitchFactor = 1,		// multiplication factor for pitch
	//		gainFactor = -3,	// multiplication factor for volume (dB)
	//		guiIndex = 1,		// index for GUI placement
	//	 	guiVisible = 1,		// 0=hide all GUI
	//	 	guiTop = 20,		// vertical offsett for GUI elements
	//	 	win;				// parent window for GUI

	wMF.onClose_({
		freezeInstance.size.do { |i|
			if(freezeInstance[i].notNil, {freezeInstance[i].stop(forced:1)});
		};
		buffer.free;
		if(~wMeterAvail.notNil, {~wMeter.close});
	});

	w = Window("dummy").front; w.close;
	s.meter;
	wMF.front;

});

)
