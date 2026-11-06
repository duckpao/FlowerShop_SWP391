import LegacyCustomRequestEntry from '../components/customFlowers/LegacyCustomRequestEntry'

export default function CustomerCustomOrdersView({ auth }) {
  return <LegacyCustomRequestEntry user={auth?.user} authBusy={auth?.busy} />
}
