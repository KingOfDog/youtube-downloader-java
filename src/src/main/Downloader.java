package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;

import javax.management.RuntimeErrorException;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.vget.vhs.VimeoInfo;
import com.github.axet.vget.vhs.YouTubeInfo;
import com.github.axet.wget.SpeedInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.thoughtworks.xstream.io.json.JsonWriter.Format;

public class Downloader implements Runnable {
	VideoInfo vi;
	long last;
	static Scene scene;
	
	Map<VideoFileInfo, SpeedInfo> map = new HashMap<VideoFileInfo, SpeedInfo>();
	
	public Downloader(VideoInfo i) {
		this.vi = i;
	}
	
	public SpeedInfo getSpeedInfo(VideoFileInfo dinfo) {
		SpeedInfo si = map.get(dinfo);
		if(si == null) {
			si = new SpeedInfo();
			si.start(dinfo.getCount());
			map.put(dinfo, si);
		}
		return si;
	}
	
	public void run() {
		List<VideoFileInfo> dinfoList = vi.getInfo();
		
		switch(vi.getState()) {
		case EXTRACTING:
		case EXTRACTING_DONE:
		case DONE:
			if(vi instanceof YouTubeInfo) {
				YouTubeInfo i = (YouTubeInfo) vi;
				System.out.println(vi.getState() + " " + i.getVideoQuality());
			} else if(vi instanceof VimeoInfo) {
				VimeoInfo i = (VimeoInfo) vi;
				System.out.println(vi.getState() + " " + i.getVideoQuality());
			} else {
				System.out.println("unkown");
			}
			for(VideoFileInfo d : vi.getInfo()) {
				SpeedInfo speedInfo = getSpeedInfo(d);
				speedInfo.end(d.getCount());
				System.out.println(String.format("file:%d - %s (%s)", dinfoList.indexOf(d), d.targetFile, formatSpeed(speedInfo.getAverageSpeed())));
			}
			break;
		case ERROR:
			System.out.println(vi.getState() + " " + vi.getDelay());
			if(dinfoList != null) {
				for(DownloadInfo dinfo : dinfoList) {
					System.out.println("file: " + dinfoList.indexOf(dinfo) + " - " + dinfo.getException() + dinfo.getDelay());
				}
			}
			break;
		case RETRYING:
			System.out.println(vi.getState() + " " + + vi.getDelay());
			if(dinfoList != null) {
				for(DownloadInfo dinfo : dinfoList) {
					System.out.println("file: " + dinfoList.indexOf(dinfo) + " - " + dinfo.getState() + " " + dinfo.getException() + "delay:" + dinfo.getDelay());
				}
			}
			break; 
		case DOWNLOADING:
			long now = System.currentTimeMillis();
			if(now - 1000 > last) {
				last = now;;
				
				String parts = "";
				
				for(VideoFileInfo dinfo : dinfoList) {
					SpeedInfo speedInfo = getSpeedInfo(dinfo);
					speedInfo.step(dinfo.getCount());
					
					List<Part> pp = dinfo.getParts();
					if(pp != null) {
						for(Part p : pp) {
							if(p.getState().equals(States.DOWNLOADING)) {
								parts += String.format("part#%d(%.2f) ", p.getNumber(), p.getCount() / (float) p.getLength());
							}
						}
					}
					TextArea console = (TextArea) scene.lookup("#console");
					String text =  String.format("file:%d - %s %.2f %s (%s)", dinfoList.indexOf(dinfo), vi.getState(), dinfo.getCount() / (float) dinfo.getLength(), parts, formatSpeed(speedInfo.getCurrentSpeed()));
					System.out.println(text);
				}
			}
			break;
		default:
			break;
		}
	}

	private static String formatSpeed(long s) {
		if(s > 0.1 * 1024 * 1024 * 1024) {
			float f = s / 1024f / 1024f / 1024f;
			return String.format("%.1f GB/s", f);
		} else if(s > 0.1 * 1024 * 1024) {
			float f = s / 1024f / 1024f;
			return String.format("%.1f MB/s", f);
		} else {
			float f = s / 1024f;
			return String.format("%.1f kb/s", f);
		}
	}

	public static void get(String url, File file, Scene thisScene) {
		try {
			final AtomicBoolean stop = new AtomicBoolean(false);
			
			scene = thisScene;
			
			URL web = new URL(url);
			
			VGetParser user = null;
			
			user = VGet.parser(web);
			
			VideoInfo videoinfo = user.info(web);
			
			VGet v = new VGet(videoinfo, file);
			
			Downloader notify = new Downloader(videoinfo);
			
			v.extract(user, stop, (Runnable) notify);
			
			System.out.println("Title: " + videoinfo.getTitle());
			Label title = (Label) scene.lookup("#videoTitle");
			title.setText(videoinfo.getTitle());
			List<VideoFileInfo> list = videoinfo.getInfo();
			if(list != null) {
				for(VideoFileInfo d : list) {
					System.out.println("Download URL: " + d.getSource());
				}
			}
			
			v.download(user, stop, (Runnable) notify);
		} catch(DownloadInterruptedError e) {
			throw e;
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
