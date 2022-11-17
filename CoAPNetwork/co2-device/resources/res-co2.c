#include <stdlib.h>
#include <time.h>
#include <string.h>

#include "contiki.h"
#include "node-id.h"
#include "coap-engine.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP



/**************** RESOURCES **********************/

#define LOWER_BOUND_CO2 	500
#define UPPER_BOUND_CO2 	3500
#define SMALL_VARIATION		100
#define BIG_VARIATION    	500

static int co2 = 1500;
static short state = 0;
static int randomica = 0;
static int decrement = 0;

//static unsigned int accept = APPLICATION_JSON;


static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
//static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_co2,
       "title=\"CO2 sensor\";rt=\"CO2\";obs",
       res_get_handler,
       NULL, //res_put_handler,
       NULL, //res_put_handler,
       NULL,
       res_event_handler);

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{

    LOG_INFO("Handling co2 get request...\n");
    unsigned int accept = -1;
    coap_get_header_accept(request, &accept);


    if(accept == -1 || accept == APPLICATION_JSON)	//only JSON format
    {
        if (co2 > UPPER_BOUND_CO2) {
        	if(state != 2) {
					state = 2;
               				LOG_WARN("CO2 level too high!\n");
					decrement = 2;
			       } 
        } else if (co2 >= LOWER_BOUND_CO2 && co2 < UPPER_BOUND_CO2) {
          	if(state != 1) {
					state = 1;
					LOG_WARN("CO2 level high!\n");
			       }
        } else {
          	if(state != 0) {
					state = 0;
					LOG_INFO("CO2 level OK!\n");
			       }
        }

	//PREPARE THE BUFFER
        snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"node_id\":%d,\"co2\":%d,\"timestamp\":%lu}", node_id, co2, clock_seconds());
        int length = strlen((char*)buffer);

        printf("%s\n", buffer);        

	// COAP FUNCTIONS
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_header_etag(response, (uint8_t *)&length, 1);
        coap_set_payload(response, buffer, length);
    }
    else {
		coap_set_status_code(response, NOT_ACCEPTABLE_4_06);
        	const char *msg = "Supported content-types:application/json";
	    	coap_set_payload(response, buffer, strlen(msg));
    }
}



static int generateRandom(int lower, int upper)
{
	randomica = rand();
	//printf("%d ", randomica);
	int num = (randomica %(upper - lower + 1)) + lower;
	printf("%d ", num);
	return num;
}

static void res_event_handler(void)
{
    // estimate new co2 level
    //srand(time(NULL) * node_id);

    int new_co2 = co2;
    int production = generateRandom(0, 9);    //production of the industry

    if (decrement > 0){
	printf("Decrement: %d ", decrement);
	new_co2 -= 1000;
	decrement--;
    }
	else {

	    if (production == 0) {
		if (new_co2 - BIG_VARIATION < 0)
			new_co2 += BIG_VARIATION;
		else
			new_co2 -= BIG_VARIATION;
	    }  
	    else if (production == 9) {
		new_co2 += BIG_VARIATION;
	    }  
	    else if (production > 0 && production < 5) {
		if (new_co2 - SMALL_VARIATION < 0)
			new_co2 += SMALL_VARIATION;
		else
			new_co2 -= SMALL_VARIATION;
	    }  
	    else if (production >= 5 && production < 9) {
		new_co2 += SMALL_VARIATION;
	    }  
    }

    // if not equal
    if (new_co2 != co2)
    {
	co2 = new_co2;
	coap_notify_observers(&res_co2);
    }
    
}
