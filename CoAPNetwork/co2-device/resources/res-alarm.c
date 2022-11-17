#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

#include <string.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example, depending on the color query parameter and post variable mode, corresponding led is activated or deactivated */
RESOURCE(res_alarm,
         "title=\"LEDs: ?color=r|g|y, POST/PUT mode=on|off\";rt=\"Control\"",
         NULL,
         res_post_put_handler,
         res_post_put_handler,
         NULL);

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    size_t len = 0;
    const char *color = NULL;
    const char *mode = NULL;
    uint8_t led = 0;
    int success = 1;
    //int decrement = 0;

    if((len = coap_get_query_variable(request, "color", &color))) 
    {
        LOG_INFO("color %.*s\n", (int)len, color);

        if(strncmp(color, "r", len) == 0) 
        {
            led = LEDS_RED;
           
        } else if(strncmp(color, "g", len) == 0) 
        {
            led = LEDS_GREEN;
        } else if(strncmp(color, "y", len) == 0) 
        {
            led = LEDS_YELLOW;
        } else 
        {
            success = 0;
        }
    } else {
        success = 0;
    } if(success && (len = coap_get_post_variable(request, "mode", &mode))) 
    {
        LOG_DBG("mode %s\n", mode);

        if(strncmp(mode, "on", len) == 0) 
        {
            leds_set(LEDS_NUM_TO_MASK(led));
            leds_on(LEDS_NUM_TO_MASK(LEDS_GREEN));
            
        } else if(strncmp(mode, "off", len) == 0) 
        {
            leds_set(LEDS_NUM_TO_MASK(led));
        } else 
        {
            success = 0;
        }
    } else {
        success = 0;
    } if(!success) 
    {
        coap_set_status_code(response, BAD_REQUEST_4_00);
    }
}
