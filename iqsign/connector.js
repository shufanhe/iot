/********************************************************************************/
/*                                                                              */
/*              connector.js                                                    */
/*                                                                              */
/*      Code for connecting device using st-schema library                      */
/*      From StartThingCommunity/st-schema-nodejs on github                     */
/*                                                                              */
/********************************************************************************/

const { SchemaConnector, DeviceErrorTypes } = require('st-schema')
const deviceStates = { switch: 'off', level: 100 }
const accessTokens = { }; 

const connector = new SchemaConnector()
  .discoveryHandler(handleDiscovery)
  .stateRefreshHandler(handleStateRefresh)
  .commandHandler(handleCommand)
  .callbackAccessHandler(handleCallbackAccess)
  .integrationDeletedHandler(handleIntegrationDeleted);

 

/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

function handleDiscovery(accesstoken, res) 
{
    res.addDevice('external-device-1', 'Test Dimmer', 'c2c-dimmer')
      .manufacturerName('Example Connector')
      .modelName('Virtual Dimmer');
}



function handleStateRefresh(accesstoken, res) 
{
    res.addDevice('external-device-1', [
    {
        component: 'main',
        capability: 'st.switch',
        attribute: 'switch',
        value: deviceStates.switch
    },
    {
        component: 'main',
        capability: 'st.switchLevel',
        attribute: 'level',
        value: deviceStates.level
    }
    ])
}


    
function handleCommand(accessToken, response, devices)
{
    for (const device of devices) {
        const deviceResponse = response.addDevice(device.externalDeviceId);
        for (const cmd of device.commands) {
            const state = {
                    component: cmd.component,
                    capability: cmd.capability
            };
            if (cmd.capability === 'st.switchLevel' && cmd.command === 'setLevel') {
                state.attribute = 'level';
                state.value = deviceStates.level = cmd.arguments[0];
                deviceResponse.addState(state);
                
            } 
            else if (cmd.capability === 'st.switch') {
                state.attribute = 'switch';
                state.value = deviceStates.switch = cmd.command === 'on' ? 'on' : 'off';
                deviceResponse.addState(state);
                
            } 
            else {
                deviceResponse.setError(
                        `Command '${cmd.command} of capability '${cmd.capability}' not supported`,
                        DeviceErrorTypes.CAPABILITY_NOT_SUPPORTED)
            }
        }
    }
}


function handleCallbackAccess(accesstoken,callbackauth,callbackurls)
{
    accessTokens[accesstoken] = { callbackauth,callbackurls };
}


function handleIntegrationDeleted(accesstoken)
{
    delete accessTokens[accesstoken];
}



/********************************************************************************/
/*                                                                              */
/*      Exports                                                                 */
/*                                                                              */
/********************************************************************************/

exports.connector = connector;
exports.deviceStates = deviceStates;
exports.accessTokens = accessTokens;

