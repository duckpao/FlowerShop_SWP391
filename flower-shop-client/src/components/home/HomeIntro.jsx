import { useEffect, useState } from 'react'
import { catalogService } from '../../services/catalogService'

const steps = [
    { title: 'Chọn hoa', text: 'Duyệt theo danh mục hoặc theo cửa hàng, xem giá và đánh giá của từng sản phẩm.' },
    { title: 'Nhập địa chỉ nhận', text: 'Chọn tỉnh, quận, phường theo danh mục của GHN để tính đúng phí giao hàng trước khi đặt.' },
    { title: 'Cửa hàng xác nhận', text: 'Shop xác nhận đơn, cắm hoa rồi tạo vận đơn giao hàng nhanh.' },
    { title: 'Theo dõi đến khi nhận', text: 'Trạng thái vận đơn cập nhật theo GHN ngay trong trang đơn hàng của bạn.' },
]

const promises = [
    { title: 'Giao bởi Giao Hàng Nhanh', text: 'Mỗi đơn có mã vận đơn riêng, tra cứu được từng chặng từ lấy hàng đến giao.' },
    { title: 'Thanh toán khi nhận hoặc trả trước', text: 'Chọn COD hoặc thanh toán online qua mã QR; đơn trả trước không bị thu tiền lần hai khi giao.' },
    { title: 'Cửa hàng được duyệt', text: 'Shop mới chỉ bán sau khi quản trị viên duyệt hồ sơ mở cửa hàng.' },
]

function Stat({ value, label }) {
    return <div>
        <dd className="text-3xl font-bold text-gray-900 dark:text-white">{value}</dd>
        <dt className="mt-1 text-sm text-gray-500 dark:text-gray-400">{label}</dt>
    </div>
}

/**
 * Introduction block under the hero. Counts come from the public catalog API, so the
 * numbers are the shop's real inventory; nothing is shown until they load.
 */
export default function HomeIntro() {
    const [categories, setCategories] = useState([])
    const [shopCount, setShopCount] = useState(0)

    useEffect(() => {
        let active = true
        catalogService.categories()
            .then(list => { if (active) setCategories(list) })
            .catch(() => { if (active) setCategories([]) })
        catalogService.shops()
            .then(list => { if (active) setShopCount(Array.isArray(list) ? list.length : 0) })
            .catch(() => { if (active) setShopCount(0) })
        return () => { active = false }
    }, [])

    const productCount = categories.reduce((sum, c) => sum + (c.productCount || 0), 0)
    const ready = categories.length > 0

    return <div className="space-y-16">
        {ready && <dl className="grid grid-cols-3 gap-6 border-y border-gray-200 py-8 text-center dark:border-white/10">
            <Stat value={productCount} label="sản phẩm đang bán" />
            <Stat value={categories.length} label="danh mục hoa" />
            {shopCount > 0 && <Stat value={shopCount} label="cửa hàng đang hoạt động" />}
        </dl>}

        <section aria-labelledby="home-steps" className="grid gap-10 lg:grid-cols-[1fr_2fr]">
            <div>
                <h2 id="home-steps" className="text-2xl font-bold text-gray-900 dark:text-white">Đặt hoa diễn ra thế nào</h2>
                <p className="mt-2 text-gray-500 dark:text-gray-400">Bốn bước, từ lúc chọn hoa đến khi người nhận cầm bó hoa trên tay.</p>
            </div>
            <ol className="space-y-6">
                {steps.map((step, index) => <li key={step.title} className="grid grid-cols-[2rem_1fr] gap-4">
                    <span className="text-lg font-semibold text-brand-500">{index + 1}</span>
                    <div>
                        <h3 className="font-semibold text-gray-900 dark:text-white">{step.title}</h3>
                        <p className="mt-1 text-sm leading-6 text-gray-600 dark:text-gray-400">{step.text}</p>
                    </div>
                </li>)}
            </ol>
        </section>

        <section aria-labelledby="home-promises" className="rounded-3xl bg-gray-900 px-6 py-10 text-white sm:px-10 dark:bg-white/5">
            <h2 id="home-promises" className="text-2xl font-bold">Vì sao đặt qua FlowerShop</h2>
            <div className="mt-8 grid gap-8 md:grid-cols-3">
                {promises.map(item => <div key={item.title}>
                    <h3 className="font-semibold">{item.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-gray-300">{item.text}</p>
                </div>)}
            </div>
        </section>
    </div>
}
