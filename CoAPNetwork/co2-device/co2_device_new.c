#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "sys/etimer.h"
#include "dev/leds.h"
#include "os/dev/serial-line.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#include "coap-engine.h"
#include "coap-blocking-api.h"

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONN_TRY_INTERVAL 1
#define REG_TRY_INTERVAL 1


#define SENSOR_TYPE "co2_sensor"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

#define LOWER_BOUND 350
#define UPPER_BOUND 5000

static struct etimer myTimer;
bool registered = false;
static bool connected = false;
//static int period = 0;

static struct etimer wait_connectivity;
static struct etimer wait_registration;

char* service_url = "/registration";

static void check_connection()
{
    if (!NETSTACK_ROUTING.node_is_reachable())
    {
        LOG_WARN("BR not reachable\n");
        etimer_reset(&wait_connectivity);
    }
    else
    {
        LOG_INFO("BR reachable\n");
        leds_set(LEDS_NUM_TO_MASK(LEDS_YELLOW));
        connected = true;
    }
}


void client_chunk_handler(coap_message_t *response)
{
	const uint8_t *chunk;

	if(response == NULL) {
		LOG_WARN("Request timed out");
		etimer_set(&wait_registration, CLOCK_SECOND * REG_TRY_INTERVAL);
		return;
	}

	int len = coap_get_payload(response, &chunk);
	if(strncmp((char*)chunk, "Registration successful!", len) == 0){
		registered = true;
		leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
	} else
		etimer_set(&wait_registration, CLOCK_SECOND * REG_TRY_INTERVAL);
}

PROCESS(co2_server, "CO2 sensor Server");
AUTOSTART_PROCESSES(&co2_server);

int co2 = 350;

extern coap_resource_t res_co2;  //fare risorsa res-co2
extern coap_resource_t res_alarm;	//modificare o no?



PROCESS_THREAD(co2_server, ev, data) 
{
	static coap_endpoint_t server_ep;
    	static coap_message_t request[1]; 

	PROCESS_BEGIN();

	leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
	etimer_set(&wait_connectivity, CLOCK_SECOND * CONN_TRY_INTERVAL);

	PROCESS_PAUSE();
	
  	LOG_INFO("Starting sensor node\n");

	while (!connected) {
		PROCESS_WAIT_UNTIL(etimer_expired(&wait_connectivity));
		check_connection();
	}
	printf("CONNECTED\n");

	coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);

	coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);

	coap_set_header_uri_path(request, service_url);


	//LOG_DBG("Registering with server\n");
	//COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);

	coap_set_payload(request, (uint8_t*) SENSOR_TYPE, sizeof(SENSOR_TYPE) - 1);

	
	while (!registered) {
		LOG_DBG("Retrying with server\n");	
	       	COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);
        	// wait for the timer to expire
        	PROCESS_WAIT_UNTIL(etimer_expired(&wait_registration));
    	}
	
	printf("REGISTERED\n");

	printf("Starting CO2 server\n");

        // RESOURCES ACTIVATION
        coap_activate_resource(&res_co2, "co2");  //mettere risorsa co2
        coap_activate_resource(&res_alarm, "alarm");  //cambiare qualcosa

	etimer_set(&myTimer, 3*CLOCK_SECOND);

	while(1) {
		PROCESS_WAIT_EVENT();

		if (ev == PROCESS_EVENT_TIMER && data == &myTimer) {
			res_co2.trigger();
			etimer_reset(&myTimer);
		}
	}
	
	PROCESS_END();
}




























