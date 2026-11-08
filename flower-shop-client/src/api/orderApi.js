import client from '../services/axiosClient'
export default {
makeOrder: (userId,body) => client.post(`/orders/${userId}`,body),
getShippingFee: (userId,body) => client.get(`/orders/${userId}/shipping-fee`,{params:{shopId:body.shopId,addressId:body.deliveryAddressId}}),
getOrderHistory: userId=>client.get(`/orders/${userId}`),
}

