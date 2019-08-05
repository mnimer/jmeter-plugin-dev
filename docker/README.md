# Running JMeter Controller & Remotes in GCP

This repository walks you through the steps of building docker containers of JMeter configured in a distributed Controller and multiple remote nodes running on GCP.

## Prerequisites

1. GCloud
2. Docker
3. Git
4. Container Repository API enabled
5. Container Repository Credentials - https://cloud.google.com/container-registry/docs/access-control

## Building your own JMeter Images and uploading it to Google Container Registry

1. Clone this repository
2. Change directory to the repository folder

### Building & Uploading the JMeter Controller Image

- Run the build process for the controller. This will take some time

    ```bash
    cd master
    ```
    
    ```bash
    gcloud builds submit --config=cloudbuild.yaml .
    ```

		
- You can now use gcr.io/[PROJECT ID]/jmeter-controller as the image in the following steps

### Building & Uploading the JMeter Remote Image

- Run the build process for the controller. This will take some time

    ```bash
    cd agent
    ```
    
    ```bash
    gcloud builds submit --config=cloudbuild.yaml .
    ```

		
- You can now use gcr.io/[PROJECT ID]/jmeter-agent as the image in the following steps


## Creating a JMeter Remote Virtual Machine (no gui)

1. Navigate to Compute Engine and create a new instance
2. Enter a name for the instance like ```jmeter-agent-central```
3. Choose the appropriate region and zone
4. Choose a machine type that has sufficient memory for your test case(s)
5. Click the Deploy a container image to this VM instance
6. Use the following container image

    ```
	  gcr.io/[PROJECT ID]/jmeter-agent
    ```
		
7. Change the Access scopes to Allow full access to all Cloud APIs if you need to access other GCP resources
8. Click the Create button

Alternatively, you can use the following gcloud command (and modify the parameters as needed)

    ```
	  gcloud beta compute --project=[PROJECT ID] instances create-with-container jmeter-agent-central --zone=us-central1-a --machine-type=n1-standard-1 --subnet=default --network-tier=PREMIUM --metadata=google-logging-enabled=true --maintenance-policy=MIGRATE --service-account=419644753316-compute@developer.gserviceaccount.com --scopes=https://www.googleapis.com/auth/cloud-platform --image=cos-stable-74-11895-125-0 --image-project=cos-cloud --boot-disk-size=10GB --boot-disk-type=pd-standard --boot-disk-device-name=jmeter-agent-central --container-image=gcr.io/[PROJECT ID]/jmeter-agent --container-restart-policy=always --labels=container-vm=cos-stable-74-11895-125-0
	```

Repeat as needed for every JMeter remote you need, remembering to change the instance name

## Creating a JMeter Controller Virtual Machine (GUI with VNC)

1. Navigate to Compute Engine and create a new instance
2. Enter a name for the instance like jmeter-controller
3. Choose the appropriate region and zone
4. Choose a machine type that has sufficient memory for your test case(s). Since this requires more memory, would recommend at least 15 GB of RAM
5. Click the Deploy a container image to this VM instance
6. Use the following container image

    ```
	  gcr.io/[PROJECT ID]/jmeter-controller
    ```
			
7. Expand the advanced contiainer options
8. Click the Add Envrionment Variable
9. Enter REMOTE_HOSTS for the name
10. Enter either the IP address(es) or host names of your jmeter remotes. For example

    ```
	   REMOTE_HOSTS	jmeter-agent    .c.[PROJECT ID].internal
    ```
			
	If you have multiple hosts, separate them using commas. You can also specify the port if it is not the normal 1099 port e.g.
	
    ```
      jmeter-remote-1.c.[PROJECT ID].internal:[port], jmeter-remote-2.c.[PROJECT ID].internal:[port]
    ```
		
11. Click the Change button next to the Boot Disk and use 20 GB for the persistent disk
12. Change the Access scopes to Allow full access to all Cloud APIs if you need to access other GCP resources
13. Click the Create button

Alternatively, you can use the following gcloud command (and modify the parameters as needed)

    ```	
	  gcloud beta compute --project=[PROJECT ID] instances create-with-container jmeter-controller --zone=us-central1-a --machine-type=n1-standard-4 --subnet=default --network-tier=PREMIUM --metadata=google-logging-enabled=true --maintenance-policy=MIGRATE --service-account=419644753316-compute@developer.gserviceaccount.com --scopes=https://www.googleapis.com/auth/cloud-platform --image=cos-stable-74-11895-125-0 --image-project=cos-cloud --boot-disk-size=20GB --boot-disk-type=pd-standard --boot-disk-device-name=jmeter-controller --container-image=gcr.io/[PROJECT ID]/jmeter-controller --container-restart-policy=always --container-env=REMOTE_HOSTS=jmeter-remote-1.c.[PROJECT ID].internal --labels=container-vm=cos-stable-74-11895-125-0
	```

## Launching a test via the GUI

1. Connect to the JMeter controller with VNC over ssh. Use the following GCloud command (modify as needed) to connect to your controller 

    ```	
	  gcloud compute --project "${PROJECT_ID}" ssh --zone "us-central1-a" "jmeter-controller" --ssh-flag="-L 5901:localhost:5901"
    ```
		
2. Make sure that docker is running the container

    ```	
	  docker ps
    ```
		
	One of the images is stackdriver agent and the other is the jmeter-controller
	
3. On your laptop, Use a VNC Viewer application on your laptop/chromebook to connect to localhost:5901 and enter the password (in the master/Dockerfile "VNC_PWD") 

4. Happy JMeter testing! 

## A few notes worth mentioning:
 * Exiting JMeter will kill the container and your VNC session. The VM will automatically restart the container, but you will not be able to reconnect without restarting the VM. To reboot the VM use:
 
    ```
 	  sudo /sbin/shutdown -r now
 	```
	
 * To access the Terminal inside of VNC, you can alt-tab to bring up the Home window. In the upper right, click the menu button on the far right and select Open in Terminal.
 * To access an editor you can either use vi or gedit from the command line, or choose Text Editor or VIM under the Applications | Accessories menu
 * You can close the VNC Viewer on your laptop and reconnect at a later time if you choose to do so.
 
## Credits

Thanks to https://hub.docker.com/r/justb4/jmeter for providing the Dockerfiles that got me on the right track. 

